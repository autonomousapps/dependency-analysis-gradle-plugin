package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.externalArtifactsFor
import com.autonomousapps.internal.graph.GraphInput
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.tasks.GraphViewTask.Companion.toFlatDeps
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage") // Guava graphs
@CacheableTask
abstract class ResolveExternalDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Resolves external dependencies for single variant."

//    if (GradleVersions.isAtLeastGradle74) {
//      @Suppress("LeakingThis")
//      notCompatibleWithConfigurationCache("Cannot serialize Configurations")
//    }
  }

//  @Transient
//  private lateinit var compileClasspath: Configuration
//
//  @Transient
//  private lateinit var runtimeClasspath: Configuration
//
//  @get:PathSensitive(PathSensitivity.NAME_ONLY)
//  @get:InputFiles
//  abstract val compileFiles: ConfigurableFileCollection
//
//  @get:PathSensitive(PathSensitivity.NAME_ONLY)
//  @get:InputFiles
//  abstract val runtimeFiles: ConfigurableFileCollection


  @get:Input
  abstract val projectPath: Property<String>

//  @Nested
//  lateinit var compileGraph: GraphInput
//
//  @Nested
//  lateinit var runtimeGraph: GraphInput

  /*
   * Compile classpath.
   */

  @get:Input
  abstract val fileDepsForCompile: ListProperty<String>

  @get:Input
  abstract val compileGraph: Property<ResolvedComponentResult>

  /*
   * Runtime classpath.
   */

  @get:Input
  abstract val fileDepsForRuntime: ListProperty<String>

  @get:Input
  abstract val runtimeGraph: Property<ResolvedComponentResult>

  /** Output in json format for compile classpath graph. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  internal fun configureTask(
    project: Project,
    compileClasspath: Configuration,
    runtimeClasspath: Configuration,
    jarAttr: String
  ) {
//    this.compileClasspath = compileClasspath
//    this.runtimeClasspath = runtimeClasspath
//    compileFiles.setFrom(project.provider { compileClasspath.externalArtifactsFor(jarAttr).artifactFiles })
//    runtimeFiles.setFrom(project.provider { runtimeClasspath.externalArtifactsFor(jarAttr).artifactFiles })

    projectPath.set(project.path)
    // TODO I want only external artifacts, but I also need a ResolvedComponentResult...
    val a = compileClasspath.externalArtifactsFor(jarAttr).artifactFiles
    fileDepsForCompile.set(compileClasspath.toFlatDeps(project))
    fileDepsForRuntime.set(runtimeClasspath.toFlatDeps(project))
    compileGraph.set(compileClasspath.incoming.resolutionResult.rootComponent)
    runtimeGraph.set(runtimeClasspath.incoming.resolutionResult.rootComponent)
  }

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val compileGraph = GraphViewBuilder(
      rootId = ProjectCoordinates(projectPath.get()),
      root = compileGraph.get(),
      fileDeps = fileDepsForCompile.get()
    ).graph
    val runtimeGraph = GraphViewBuilder(
      rootId = ProjectCoordinates(projectPath.get()),
      root = runtimeGraph.get(),
      fileDeps = fileDepsForRuntime.get()
    ).graph

    val dependencies = compileGraph.nodes().asSequence().plus(runtimeGraph.nodes().asSequence())
      .filterIsInstance<ModuleCoordinates>()
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
