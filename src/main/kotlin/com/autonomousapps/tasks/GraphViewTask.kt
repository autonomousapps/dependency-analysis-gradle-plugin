// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.externalArtifactsFor
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.CoordinatesContainer
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.source.SourceKind
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

@CacheableTask
public abstract class GraphViewTask : DefaultTask() {

  init {
    description = "Constructs a variant-specific view of this project's dependency graph"
  }

  @get:Internal
  public abstract val compileClasspathName: Property<String>

  @get:Internal
  public abstract val compileClasspathResult: Property<ResolvedComponentResult>

  @get:Internal
  public abstract val compileClasspathFileCoordinates: SetProperty<Coordinates>

  @get:Internal
  public abstract val runtimeClasspathName: Property<String>

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

  /**
   * Unused, except to influence the up-to-date-ness of this task. Declaring a transitive dependency doesn't change the
   * compile classpath, but it must influence the output of this task.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val declarations: RegularFileProperty

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  public abstract val buildPath: Property<String>

  /** Needed to disambiguate other projects that might have otherwise identical inputs. */
  @get:Input
  public abstract val projectPath: Property<String>

  @get:Input
  public abstract val sourceKind: Property<SourceKind>

  /** Output in json format for compile classpath graph. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  /** Output in json format for compile classpath dependencies (the graph's nodes). */
  @get:OutputFile
  public abstract val outputNodes: RegularFileProperty

  /** Output in graphviz format for compile classpath graph. */
  @get:OutputFile
  public abstract val outputDot: RegularFileProperty

  /** Output in json format for runtime classpath graph. */
  @get:OutputFile
  public abstract val outputRuntime: RegularFileProperty

  /** Output in graphviz format for runtime classpath graph. */
  @get:OutputFile
  public abstract val outputRuntimeDot: RegularFileProperty

  internal fun configureTask(
    project: Project,
    compileClasspath: Configuration,
    runtimeClasspath: Configuration,
    jarAttr: String,
  ) {
    compileClasspathName.set(compileClasspath.name)
    compileClasspathResult.set(compileClasspath.incoming.resolutionResult.rootComponent)
    compileClasspathFileCoordinates.set(project.provider {
      compileClasspath.allDependencies
        .filterIsInstance<FileCollectionDependency>()
        .mapNotNullToSet { it.toCoordinates() }
    })

    runtimeClasspathName.set(runtimeClasspath.name)
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
    val outputDot = outputDot.getAndDelete()
    val outputNodes = outputNodes.getAndDelete()
    val outputRuntime = outputRuntime.getAndDelete()
    val outputRuntimeDot = outputRuntimeDot.getAndDelete()

    val sourceKind = sourceKind.get()

    val compileGraph = GraphViewBuilder(compileClasspathResult.get(), compileClasspathFileCoordinates.get()).graph
    val compileGraphView = DependencyGraphView(
      sourceKind = sourceKind,
      configurationName = compileClasspathName.get(),
      graph = compileGraph
    )

    val runtimeGraph = GraphViewBuilder(runtimeClasspathResult.get(), runtimeClasspathFileCoordinates.get()).graph
    val runtimeGraphView = DependencyGraphView(
      sourceKind = sourceKind,
      configurationName = runtimeClasspathName.get(),
      graph = runtimeGraph
    )
    val graphWriter = GraphWriter(buildPath.get())

    output.bufferWriteJson(compileGraphView)
    outputDot.writeText(graphWriter.toDot(compileGraph))
    outputNodes.bufferWriteJson(CoordinatesContainer(compileGraphView.nodes.toSortedSet()))
    outputRuntime.bufferWriteJson(runtimeGraphView)
    outputRuntimeDot.writeText(graphWriter.toDot(runtimeGraph))
  }
}
