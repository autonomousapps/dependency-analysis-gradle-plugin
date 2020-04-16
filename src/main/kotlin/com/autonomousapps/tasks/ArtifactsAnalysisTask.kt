@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.DependencyConfiguration
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
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
abstract class ArtifactsAnalysisTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of all classes referenced by a given jar"
  }

  private lateinit var artifacts: ArtifactCollection

  fun setArtifacts(artifacts: ArtifactCollection) {
    this.artifacts = artifacts
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused.
   */
  @Classpath
  fun getArtifactFiles() = artifacts.artifactFiles

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val dependencyConfigurations: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction
  fun action() {
    val reportFile = output.get().asFile
    val reportPrettyFile = outputPretty.get().asFile

    // Cleanup prior execution
    reportFile.delete()
    reportPrettyFile.delete()

    val candidates = dependencyConfigurations.get().asFile.readText().fromJsonSet<DependencyConfiguration>()

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
