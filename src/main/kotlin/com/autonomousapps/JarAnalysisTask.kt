@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClassAnalyzer
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.objectweb.asm.ClassReader
import org.slf4j.LoggerFactory
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

interface ClassAnalysisTask : Task {
    @get:OutputFile
    val output: RegularFileProperty
}

/**
 * Produces a report of all classes referenced by a given jar.
 */
@CacheableTask
open class JarAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask(), ClassAnalysisTask {

    init {
        group = "verification"
        description = "Produces a report of all classes referenced by a given jar"
    }

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val jar: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    override val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val jarFile = jar.get().asFile

        workerExecutor.noIsolation().submit(JarAnalysisWorkAction::class.java) {
            jar = jarFile
            report = reportFile
        }
        workerExecutor.await()

        logger.debug("Report:\n${reportFile.readText()}")
    }
}

interface JarAnalysisParameters : WorkParameters {
    // TODO replace with val / properties?
    var jar: File
    var report: File
}

abstract class JarAnalysisWorkAction : WorkAction<JarAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(JarAnalysisWorkAction::class.java)

    // TODO some jars only have metadata. What to do about them?
    // TODO e.g. kotlin-stdlib-common-1.3.50.jar
    // TODO e.g. legacy-support-v4-1.0.0/jars/classes.jar
    override fun execute() {
        val z = ZipFile(parameters.jar)

        val classNames = z.entries().toList() // TODO asSequence()?
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { classEntry ->
                val classNameCollector = ClassAnalyzer(logger)
                val reader = z.getInputStream(classEntry).use { ClassReader(it.readBytes()) }
                reader.accept(classNameCollector, 0)
                classNameCollector
            }
            .flatMap { it.classes() }
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

/**
 * Produces a report of all classes referenced by a given set of class files.
 */
@CacheableTask
open class ClassListAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask(), ClassAnalysisTask {

    init {
        group = "verification"
        description = "Produces a report of all classes referenced by a given jar"
    }

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val kotlinClasses: FileCollection = objects.fileCollection()

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    val javaClasses: DirectoryProperty = objects.directoryProperty()

    @get:OutputFile
    override val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        // TODO use matching {} instead
        val inputFiles = javaClasses.asFileTree.plus(kotlinClasses).files
            .filter { it.path.contains("com/seattleshelter") }

        workerExecutor.noIsolation().submit(ClassListAnalysisWorkAction::class.java) {
            classes = inputFiles
            report = reportFile
        }
        workerExecutor.await()

        logger.debug("Report:\n${reportFile.readText()}")
    }
}

interface ClassListAnalysisParameters : WorkParameters {
    // TODO replace with val / properties?
    var classes: List<File>
    var report: File
}

abstract class ClassListAnalysisWorkAction : WorkAction<ClassListAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(JarAnalysisWorkAction::class.java)

    // TODO some jars only have metadata. What to do about them?
    // TODO e.g. kotlin-stdlib-common-1.3.50.jar
    // TODO e.g. legacy-support-v4-1.0.0/jars/classes.jar
    override fun execute() {
        val classNames = parameters.classes // TODO asSequence()?
            .map { classFile ->
                val classNameCollector = ClassAnalyzer(logger)
                val reader = classFile.inputStream().use { ClassReader(it) }
                reader.accept(classNameCollector, 0)
                classNameCollector
            }
            .flatMap { it.classes() }
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
