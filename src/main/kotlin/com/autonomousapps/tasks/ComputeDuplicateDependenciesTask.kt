package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.bufferWriteJsonMapSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.SortedSet

@CacheableTask
abstract class ComputeDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes 'duplicate' external dependencies across entire build."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val resolvedDependenciesReports: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val map = sortedMapOf<String, SortedSet<String>>()

    resolvedDependenciesReports.files
      .flatMap { it.readLines() }
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
