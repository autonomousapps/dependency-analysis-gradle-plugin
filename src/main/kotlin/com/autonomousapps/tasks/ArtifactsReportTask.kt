@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.filterNonGradle
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import com.autonomousapps.model.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of all the artifacts required to build the given project; i.e., the artifacts on the compile
 * classpath, the runtime classpath, and a few others. See
 * [Locator] for the full list of analyzed [Configuration][org.gradle.api.artifacts.Configuration]s. These artifacts are
 * physical files on disk, such as jars.
 */
@CacheableTask
abstract class ArtifactsReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  private lateinit var compileArtifacts: ArtifactCollection

  /**
   * This artifact collection is the result of resolving the compile classpath.
   */
  fun setCompileClasspath(compileArtifacts: ArtifactCollection) {
    this.compileArtifacts = compileArtifacts
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused. This needs to use [InputFiles] and [PathSensitivity.ABSOLUTE] because the path to the
   * jars really does matter here. Using [Classpath] is an error, as it looks only at content and
   * not name or path, and we really do need to know the actual path to the artifact, even if its
   * contents haven't changed.
   */
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @InputFiles
  fun getCompileClasspathArtifactFiles(): FileCollection = compileArtifacts.artifactFiles

  /**
   * [PhysicalArtifact]s used to compile main source.
   */
  @get:OutputFile
  abstract val output: RegularFileProperty

  /**
   * Pretty-formatted version of [output]. Useful for quick debugging.
   */
  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction
  fun action() {
    val reportFile = output.getAndDelete()
    val reportPrettyFile = outputPretty.getAndDelete()

    val allArtifacts = toPhysicalArtifacts(compileArtifacts)

    reportFile.writeText(allArtifacts.toJson())
    reportPrettyFile.writeText(allArtifacts.toPrettyString())
  }

  private fun toPhysicalArtifacts(artifacts: ArtifactCollection): Set<PhysicalArtifact> {
    return artifacts.asSequence()
      .filterNonGradle()
      .mapNotNull {
        try {
          PhysicalArtifact.of(
            artifact = it,
            file = it.file
          )
        } catch (e: GradleException) {
          null
        }
      }
      .toSortedSet()
  }
}
