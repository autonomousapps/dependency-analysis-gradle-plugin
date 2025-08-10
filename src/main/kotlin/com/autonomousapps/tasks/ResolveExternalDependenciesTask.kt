// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.externalArtifactsFor
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

@Suppress("UnstableApiUsage") // Guava graphs
@CacheableTask
public abstract class ResolveExternalDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Resolves external dependencies for compile and runtime classpaths."
  }

  @get:Internal
  public abstract val compileClasspathResult: Property<ResolvedComponentResult>

  @get:Internal
  public abstract val compileClasspathFileCoordinates: SetProperty<Coordinates>

  @get:Internal
  public abstract val runtimeClasspathResult: Property<ResolvedComponentResult>

  @get:Internal
  public abstract val runtimeClasspathFileCoordinates: SetProperty<Coordinates>

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  public abstract val compileFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  public abstract val runtimeFiles: ConfigurableFileCollection

  /** Output in flat txt format for external dependencies on compile and runtime classpaths. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  internal fun configureTask(
    project: Project,
    compileClasspath: Configuration,
    runtimeClasspath: Configuration,
    jarAttr: String,
  ) {
    compileClasspathResult.set(compileClasspath.incoming.resolutionResult.rootComponent)
    compileClasspathFileCoordinates.set(project.provider {
      compileClasspath.allDependencies
        .filterIsInstance<FileCollectionDependency>()
        .mapNotNullToSet { it.toCoordinates() }
    })

    runtimeClasspathResult.set(runtimeClasspath.incoming.resolutionResult.rootComponent)
    runtimeClasspathFileCoordinates.set(project.provider {
      runtimeClasspath.allDependencies
        .filterIsInstance<FileCollectionDependency>()
        .mapNotNullToSet { it.toCoordinates() }
    })

    compileFiles.setFrom(project.provider { compileClasspath.externalArtifactsFor(jarAttr).artifactFiles })
    runtimeFiles.setFrom(project.provider { runtimeClasspath.externalArtifactsFor(jarAttr).artifactFiles })
  }

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val compileGraph = GraphViewBuilder(compileClasspathResult.get(), compileClasspathFileCoordinates.get()).graph
    val runtimeGraph = GraphViewBuilder(runtimeClasspathResult.get(), runtimeClasspathFileCoordinates.get()).graph

    val dependencies = compileGraph.nodes().asSequence().plus(runtimeGraph.nodes().asSequence())
      .filterIsInstance<ModuleCoordinates>()
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
