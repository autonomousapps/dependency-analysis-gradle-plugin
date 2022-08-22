package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class ComputeResolvedDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Computes resolved external dependencies for all variants."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val dependencyGraphViews: ListProperty<RegularFile>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    // TODO figure out data structure
    val dependencyGraph = dependencyGraphViews.get()
      .map { it.fromJson<DependencyGraphView>() }
      .associateBy { "${it.name},${it.configurationName}" }
    val dependencies = dependencyGraph.values
      .asSequence()
      .flatMap { it.nodes }
      .filterIsInstance<ModuleCoordinates>()
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
