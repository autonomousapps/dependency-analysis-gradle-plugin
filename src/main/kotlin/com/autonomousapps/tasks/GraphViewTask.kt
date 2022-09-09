package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage")
@CacheableTask
abstract class GraphViewTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Constructs a variant-specific view of this project's dependency graph"
  }

  fun setCompileClasspath(compileClasspath: Configuration) {
    compileClasspathName.set(compileClasspath.name)
    compileGraph.set(compileClasspath.incoming.resolutionResult.rootComponent)
    fileDepsForCompile.set(compileClasspath.toFlatDeps(project))
  }

  fun setRuntimeClasspath(runtimeClasspath: Configuration) {
    runtimeClasspathName.set(runtimeClasspath.name)
    runtimeGraph.set(runtimeClasspath.incoming.resolutionResult.rootComponent)
    fileDepsForRuntime.set(runtimeClasspath.toFlatDeps(project))
  }

  // TODO move
  companion object {
    internal fun Configuration.toFlatDeps(project: Project) = project.providers.provider {
      allDependencies
        .filterIsInstance<FileCollectionDependency>()
        .mapNotNull { it.toCoordinates() }
        .map { it.gav() }
    }
  }

  /*
   * Compile classpath.
   */

  @get:Input
  abstract val compileClasspathName: Property<String>

  @get:Input
  abstract val fileDepsForCompile: ListProperty<String>

  @get:Input
  abstract val compileGraph: Property<ResolvedComponentResult>

  /*
   * Runtime classpath.
   */

  @get:Input
  abstract val runtimeClasspathName: Property<String>

  @get:Input
  abstract val fileDepsForRuntime: ListProperty<String>

  @get:Input
  abstract val runtimeGraph: Property<ResolvedComponentResult>

  /**
   * Unused, except to influence the up-to-date-ness of this task. Declaring a transitive dependency doesn't change the
   * compile classpath, but it must influence the output of this task.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  /** Needed to disambiguate other projects that might have otherwise identical inputs. */
  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val variant: Property<String>

  @get:Input
  abstract val kind: Property<SourceSetKind>

  /** Output in json format for compile classpath graph. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  /** Output in graphviz format for compile classpath graph. */
  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  /** Output in json format for runtime classpath graph. */
  @get:OutputFile
  abstract val outputRuntime: RegularFileProperty

  /** Output in graphviz format for runtime classpath graph. */
  @get:OutputFile
  abstract val outputRuntimeDot: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()
    val outputDot = outputDot.getAndDelete()
    val outputRuntime = outputRuntime.getAndDelete()
    val outputRuntimeDot = outputRuntimeDot.getAndDelete()

    val compileGraph = GraphViewBuilder(
      rootId = ProjectCoordinates(projectPath.get()),
      root = compileGraph.get(),
      fileDeps = fileDepsForCompile.get(),
    ).graph
    val compileGraphView = DependencyGraphView(
      variant = Variant(variant.get(), kind.get()),
      configurationName = compileClasspathName.get(),
      graph = compileGraph
    )

    val runtimeGraph = GraphViewBuilder(
      rootId = ProjectCoordinates(projectPath.get()),
      root = runtimeGraph.get(),
      fileDeps = fileDepsForRuntime.get(),
    ).graph
    val runtimeGraphView = DependencyGraphView(
      variant = Variant(variant.get(), kind.get()),
      configurationName = runtimeClasspathName.get(),
      graph = runtimeGraph
    )

    output.writeText(compileGraphView.toJson())
    outputDot.writeText(GraphWriter.toDot(compileGraph))
    outputRuntime.writeText(runtimeGraphView.toJson())
    outputRuntimeDot.writeText(GraphWriter.toDot(runtimeGraph))
  }
}
