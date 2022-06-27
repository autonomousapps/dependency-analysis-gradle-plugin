package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
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

  @get:Internal
  abstract val jarAttr: Property<String>

  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getCompileClasspath(): FileCollection = compileClasspath
    .artifactsFor(jarAttr.get())
    .artifactFiles

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

  /** Output in json format. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  /** Output in graphviz format. */
  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()
    val outputDot = outputDot.getAndDelete()

    val graph = GraphViewBuilder(compileClasspath).graph
    val graphView = DependencyGraphView(
      variant = Variant(variant.get(), kind.get()),
      configurationName = compileClasspath.name,
      graph = graph
    )

    output.writeText(graphView.toJson())
    outputDot.writeText(GraphWriter.toDot(graph))
  }
}
