@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.DependencyConfiguration
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of all the artifacts depended-on by the given project.
 * Uses `${variant}CompileClasspath`, which has visibility of direct and transitive dependencies
 * (except those hidden behind `implementation`), including `compileOnly`.
 *
 * nb: this task cannot (easily) use Workers, since an [ArtifactCollection] is not serializable.
 */
@CacheableTask
abstract class ArtifactsReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  private lateinit var artifacts: ArtifactCollection

  /**
   * This artifact collection is the result of resolving the compilation classpath.
   */
  fun setArtifacts(artifacts: ArtifactCollection) {
    this.artifacts = artifacts
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused. This needs to use [InputFiles] and [PathSensitivity.RELATIVE] because the path to the
   * jars really does matter here. Using [Classpath] is an error, as it looks only at content and
   * not name or path, and we really do need to know the actual path to the artifact, even if its
   * contents haven't changed.
   */
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  fun getArtifactFiles(): FileCollection = artifacts.artifactFiles

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val dependencyConfigurations: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction
  fun action() {
    val reportFile = output.getAndDelete()
    val reportPrettyFile = outputPretty.getAndDelete()

    val candidates = dependencyConfigurations.fromJsonSet<DependencyConfiguration>()

    val artifacts = artifacts.mapNotNull {
      try {
        Artifact(
          componentIdentifier = it.id.componentIdentifier,
          file = it.file,
          candidates = candidates
        )
      } catch (e: GradleException) {
        null
      }
    }

    reportFile.writeText(artifacts.toJson())
    reportPrettyFile.writeText(artifacts.toPrettyString())
  }
}
