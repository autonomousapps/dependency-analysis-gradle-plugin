@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.ClassAnalyzer
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.objectweb.asm.ClassReader
import org.slf4j.Logger
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
    var jar: File
    var report: File
}

abstract class JarAnalysisWorkAction : WorkAction<JarAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(JarAnalysisWorkAction::class.java)

    // TODO some jars only have metadata. What to do about them?
    // 1. e.g. kotlin-stdlib-common-1.3.50.jar
    // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
    override fun execute() {
        val z = ZipFile(parameters.jar)

        val classNames = z.entries().toList()
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { classEntry -> z.getInputStream(classEntry).use { ClassReader(it.readBytes()) } }
            .collectClassNames(logger)

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
    val kotlinClasses: ConfigurableFileCollection = objects.fileCollection()

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val javaClasses: ConfigurableFileCollection = objects.fileCollection()

    @get:OutputFile
    override val output: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        val reportFile = output.get().asFile

        // Cleanup prior execution
        reportFile.delete()

        val inputClassFiles = javaClasses.asFileTree.plus(kotlinClasses)
            .filter { it.isFile && it.name.endsWith(".class") }
            .files

        workerExecutor.noIsolation().submit(ClassListAnalysisWorkAction::class.java) {
            classes = inputClassFiles
            report = reportFile
        }
        workerExecutor.await()

        logger.debug("Report:\n${reportFile.readText()}")
    }
}

interface ClassListAnalysisParameters : WorkParameters {
    var classes: Set<File>
    var report: File
}

abstract class ClassListAnalysisWorkAction : WorkAction<ClassListAnalysisParameters> {

    private val logger = LoggerFactory.getLogger(JarAnalysisWorkAction::class.java)

    // TODO some jars only have metadata. What to do about them?
    // 1. e.g. kotlin-stdlib-common-1.3.50.jar
    // 2. e.g. legacy-support-v4-1.0.0/jars/classes.jar
    override fun execute() {
        val classNames = parameters.classes
            .map { classFile -> classFile.inputStream().use { ClassReader(it) } }
            .collectClassNames(logger)

        parameters.report.writeText(classNames.joinToString(separator = "\n"))
    }
}

private fun Iterable<ClassReader>.collectClassNames(logger: Logger): Set<String> {
    return map {
        val classNameCollector = ClassAnalyzer(logger)
        it.accept(classNameCollector, 0)
        classNameCollector
    }
        .flatMap { it.classes() }
        .filterNot {
            // Filter out `java` packages, but not `javax`
            it.startsWith("java/")
        }
        .map { it.replace("/", ".") }
        .toSortedSet()
}
