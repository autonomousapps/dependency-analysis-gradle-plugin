@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClassPrinter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.objectweb.asm.ClassReader
import org.slf4j.LoggerFactory
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Produces a report of all classes referenced by a given jar.
 */
@CacheableTask
open class ClassAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of all classes referenced by a given jar"
    }

    @get:InputFile
    val jar: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val jarFile = jar.get().asFile

        workerExecutor.noIsolation().submit(ClassAnalysisWorkAction::class.java) {
            jar = jarFile
            report = reportFile
        }
        workerExecutor.await()

        logger.debug("Report:\n${reportFile.readText()}")
    }
}

interface ClassAnalysisParameters : WorkParameters {
    // TODO replace with val / properties?
    var jar: File
    var report: File
}

abstract class ClassAnalysisWorkAction : WorkAction<ClassAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(ClassAnalysisWorkAction::class.java)

    // TODO some jars only have metadata. What to do about them?
    // TODO e.g. kotlin-stdlib-common-1.3.50.jar
    // TODO e.g. legacy-support-v4-1.0.0/jars/classes.jar
    override fun execute() {
        val z = ZipFile(parameters.jar)

        val classNames = z.entries().toList() // TODO asSequence()?
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { classEntry ->
                val classNameCollector = ClassPrinter(logger)
                val reader = ClassReader(z.getInputStream(classEntry).readBytes())
                reader.accept(classNameCollector, 0)
                classNameCollector
            }
            .flatMap { it.classes }
            .filterNot {
                // Filter out `java` packages, but not `javax`
                it.startsWith("java/")
            }
            .toSet()
            .map { it.replace("/", ".") }
            .sorted() // TODO not strictly necessary post-spike

        parameters.report.writeText(classNames.joinToString(separator = "\n"))
    }
}
