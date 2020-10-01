@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.Manifest
import com.autonomousapps.internal.Res
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Takes as input two types of artifacts:
 * 1. "android-symbol-with-package-name", which resolve to files with names like "package-aware-r.txt"; and
 * 2. "android-manifest", which resolve to AndroidManifest.xml files from upstream (depending) Android libraries.
 *
 * From these inputs we compute the _import statement_ for resources contributed by Android libraries. We then parse the
 * third input, viz., the set of source files of the current module/project, looking for these imports. This produces
 * the only output, which is the set of [Dependency]s that contribute _used_ (by Java/Kotlin source) Android resources.
 *
 * An important caveat to this approach is that it will not capture resources which are used from a merged resource
 * file. That is, if you import a resource from your own package namespace (`my.package.R`), then this algorithm will
 * not detect that.
 *
 * nb: this task can't use Workers (I think), because its main inputs are [ArtifactCollection]s, and they are not
 * serializable.
 */
@CacheableTask
abstract class AndroidResToSourceAnalysisTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all resources used by Java or Kotlin source"
  }

  private lateinit var resources: ArtifactCollection

  fun setResources(resources: ArtifactCollection) {
    this.resources = resources
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused.
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getResourceArtifactFiles(): FileCollection = resources.artifactFiles

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val manifestPackages: RegularFileProperty

  /**
   * Source code. Parsed for import statements.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val javaAndKotlinSourceFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    val outputFile = output.getAndDelete()

    val packages = manifestPackages.fromJsonList<Manifest>()

    val manifestCandidates = packages.mapToSet {
      Res(dependency = it.dependency, import = "${it.packageName}.R")
    }

    val resourceCandidates = resources.mapNotNullToSet { rar ->
      try {
        extractResImportFromResFile(rar.file)?.let {
          Res(componentIdentifier = rar.id.componentIdentifier, import = it)
        }
      } catch (e: GradleException) {
        null
      }
    }

    val allCandidates = (manifestCandidates + resourceCandidates)

    val usedResources = mutableSetOf<Dependency>()
    javaAndKotlinSourceFiles.map {
      it.readLines()
    }.forEach { lines ->
      allCandidates.forEach { res ->
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
}
