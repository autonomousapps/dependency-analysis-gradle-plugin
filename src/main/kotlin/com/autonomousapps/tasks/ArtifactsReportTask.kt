@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterNonGradle
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Produces a report of all the artifacts required to build the given project; i.e., the artifacts on the compile
 * classpath, the runtime classpath, and a few others. See
 * [FindDeclarationsTask.Locator] for the full list of analyzed [Configuration][org.gradle.api.artifacts.Configuration]s. These artifacts are
 * physical files on disk, such as jars.
 */
@CacheableTask
abstract class ArtifactsReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  private lateinit var artifacts: ArtifactCollection

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  abstract val buildPath: Property<String>

  /**
   * This artifact collection is the result of resolving the compile or runtime classpath.
   */
  fun setClasspath(artifacts: ArtifactCollection) {
    this.artifacts = artifacts
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
  fun getClasspathArtifactFiles(): FileCollection = artifacts.artifactFiles

  /**
   * [PhysicalArtifact]s used to compile or run main source.
   */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    val reportFile = output.getAndDelete()

    val allArtifacts = toPhysicalArtifacts(artifacts)

    reportFile.bufferWriteJsonSet(allArtifacts)
  }

  private fun toPhysicalArtifacts(artifacts: ArtifactCollection): Set<PhysicalArtifact> {
    return artifacts.asSequence()
      .filterNonGradle()
      .mapNotNull {
        try {
          // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/948#issuecomment-1711177139
          val file = if (it.file.path.endsWith("kotlin/main")) {
            it.file.parentFile!!.parentFile!!
          } else {
            it.file
          }
          PhysicalArtifact.of(
            artifact = it,
            file = file
          )
        } catch (e: GradleException) {
          null
        }
      }
      .toSortedSet()
  }
}
