package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.readLines
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class ComputeResolvedDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes resolved external dependencies for all variants."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val externalDependencies: ListProperty<RegularFile>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val dependencies: Set<ModuleCoordinates> = externalDependencies.get()
      .asSequence()
      .flatMap { it.readLines() }
      .map {
        val external = Coordinates.of(it)
        check(external is ModuleCoordinates) { "ModuleCoordinates expected. Was $it." }
        external
      }
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
