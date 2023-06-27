package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.CoordinatesContainer
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GraphViewTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Constructs a variant-specific view of this project's dependency graph"

    if (GradleVersions.isAtLeastGradle74) {
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Cannot serialize Configurations")
    }
  }

  @Transient
  private lateinit var compileClasspath: Configuration

  fun setCompileClasspath(compileClasspath: Configuration) {
    this.compileClasspath = compileClasspath
  }

  @Transient
  private lateinit var runtimeClasspath: Configuration

  fun setRuntimeClasspath(runtimeClasspath: Configuration) {
    this.runtimeClasspath = runtimeClasspath
  }

  @get:Internal
  abstract val jarAttr: Property<String>

  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getCompileClasspath(): FileCollection = compileClasspath
    .artifactsFor(jarAttr.get())
    .artifactFiles

  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getRuntimeClasspath(): FileCollection = runtimeClasspath
    .artifactsFor(jarAttr.get())
    .artifactFiles

  /**
   * Unused, except to influence the up-to-date-ness of this task. Declaring a transitive dependency doesn't change the
   * compile classpath, but it must influence the output of this task.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declarations: RegularFileProperty

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  abstract val buildPath: Property<String>

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

  /** Output in json format for compile classpath dependencies (the graph's nodes). */
  @get:OutputFile
  abstract val outputNodes: RegularFileProperty

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
    val outputNodes = outputNodes.getAndDelete()
    val outputRuntime = outputRuntime.getAndDelete()
    val outputRuntimeDot = outputRuntimeDot.getAndDelete()

    val compileGraph = GraphViewBuilder(compileClasspath).graph
    val compileGraphView = DependencyGraphView(
      variant = Variant(variant.get(), kind.get()),
      configurationName = compileClasspath.name,
      graph = compileGraph
    )

    val runtimeGraph = GraphViewBuilder(runtimeClasspath).graph
    val runtimeGraphView = DependencyGraphView(
      variant = Variant(variant.get(), kind.get()),
      configurationName = runtimeClasspath.name,
      graph = runtimeGraph
    )

    output.bufferWriteJson(compileGraphView)
    outputDot.writeText(GraphWriter.toDot(compileGraph))
    outputNodes.bufferWriteJson(CoordinatesContainer(compileGraphView.nodes))
    outputRuntime.bufferWriteJson(runtimeGraphView)
    outputRuntimeDot.writeText(GraphWriter.toDot(runtimeGraph))
  }
}
