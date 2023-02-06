package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.utils.bufferWriteJsonMap
import com.autonomousapps.internal.utils.bufferWriteJsonMapSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.*

@CacheableTask
abstract class ComputeDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Computes 'duplicate' external dependencies across entire build."

    if (GradleVersions.isAtLeastGradle74) {
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Cannot serialize Configurations")
    }
  }

  @Transient
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var resolvedDependenciesReports: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val map = sortedMapOf<String, SortedSet<String>>()

    resolvedDependenciesReports.dependencies.asSequence()
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      .flatMap { dependency ->
        resolvedDependenciesReports.fileCollection(dependency)
          .singleOrNull { it.exists() }
          ?.readLines()
          .orEmpty()
      }
      .map {
        val external = Coordinates.of(it)
        check(external is ModuleCoordinates) { "ModuleCoordinates expected. Was $it." }
        external
      }
      .forEach {
        map.merge(it.identifier, sortedSetOf(it.resolvedVersion)) { acc, inc ->
          acc.apply { addAll(inc) }
        }
      }

    output.bufferWriteJsonMapSet(map)
  }
}
