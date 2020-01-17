@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.Dependency
import com.autonomousapps.internal.Res
import com.autonomousapps.internal.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

// TODO use workexecutor
@CacheableTask
open class AndroidResAnalysisTask @Inject constructor(
    objects: ObjectFactory,
    private val workExecutor: WorkerExecutor
) : DefaultTask() {

    /**
     * This is the "official" input for wiring task dependencies correctly, but is otherwise
     * unused.
     */
    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val artifactFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:Internal
    lateinit var resources: ArtifactCollection

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val javaAndKotlinSourceFiles: ConfigurableFileCollection = objects.fileCollection()

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    val androidManifest = objects.fileProperty()

    @get:OutputFile
    val usedAndroidResDependencies = objects.fileProperty()

    @TaskAction
    fun action() {
        val outputFile = usedAndroidResDependencies.get().asFile
        outputFile.delete()

        //val manifestImport = calculateImportFromManifestIfAvailable()

        val resourceCandidates = resources.mapNotNull { rar ->
            try {
                extractResImportFromResFile(rar.file)?.let {
                    Res(componentIdentifier = rar.id.componentIdentifier, import = it)
                }
            } catch (e: GradleException) {
                null
            }
        }.toSet()

        val usedResources = mutableSetOf<Dependency>()
        javaAndKotlinSourceFiles.map {
            it.readLines()
        }.forEach { lines ->
            resourceCandidates.forEach { res ->
                lines.find { line -> line.startsWith("import ${res.import}") }?.let {
                    usedResources.add(res.dependency)
                }
            }
        }

        outputFile.writeText(usedResources.toJson())
    }

    private fun extractResImportFromResFile(resFile: File): String? {
        val pn = resFile.useLines { it.firstOrNull() } ?: return null
        return "$pn.R"
    }

    // TODO Failed attempt to find a workaround for AGP 3.5.3
    private fun calculateImportFromManifestIfAvailable(): String? {
        val manifest = androidManifest.orNull?.asFile ?: return null

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifest)
        document.documentElement.normalize()

        val pn = document.getElementsByTagName("manifest").item(0)
            .attributes
            .getNamedItem("package")
            .nodeValue

        return "$pn.R"
    }
}
