@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.Location
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of all the artifacts required to build the given project; i.e., the artifacts on the compile
 * classpath, the runtime classpath, and a few others. See
 * [ConfigurationsToDependenciesTransformer][com.autonomousapps.internal.ConfigurationsToDependenciesTransformer] for
 * the full list of analyzed [Configuration][org.gradle.api.artifacts.Configuration]s. These artifacts are physical
 * files on disk, such as jars.
 */
@CacheableTask
abstract class ArtifactsReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  private lateinit var mainArtifacts: ArtifactCollection

  /**
   * This artifact collection is the result of resolving the compile classpath.
   */
  fun setMainArtifacts(mainArtifacts: ArtifactCollection) {
    this.mainArtifacts = mainArtifacts
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
  fun getMainArtifactFiles(): FileCollection = mainArtifacts.artifactFiles

  private var testArtifacts: ArtifactCollection? = null

  fun setTestArtifacts(testArtifacts: ArtifactCollection?) {
    this.testArtifacts = testArtifacts
  }

  /**
   * May be absent if, e.g., Android unit tests are disabled for some variant.
   *
   * See also [getMainArtifactFiles].
   */
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @InputFiles
  fun getTestArtifactFiles(): FileCollection? = testArtifacts?.artifactFiles

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val locations: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction
  fun action() {
    val reportFile = output.getAndDelete()
    val reportPrettyFile = outputPretty.getAndDelete()

    val (candidates, exclusions) = locations.fromJsonSet<Location>().partitionToSets { it.isInteresting }

    fun ArtifactCollection.asArtifacts(): Set<Artifact> = filterNonGradle()
      .mapNotNull {
        try {
          Artifact(
            componentIdentifier = it.id.componentIdentifier,
            file = it.file,
            candidates = candidates
          )
        } catch (e: GradleException) {
          null
        }
      }.filterToSet { art ->
        exclusions.none { ex ->
          art.dependency.identifier == ex.identifier
        }
      }

    val mainArtifacts = mainArtifacts.asArtifacts()
    val testArtifacts = testArtifacts?.asArtifacts().orEmpty()
    val allArtifacts = mainArtifacts + testArtifacts

    reportFile.writeText(allArtifacts.toJson())
    reportPrettyFile.writeText(allArtifacts.toPrettyString())
  }
}
