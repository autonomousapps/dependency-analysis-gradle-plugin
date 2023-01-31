package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.externalArtifactsFor
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@Suppress("UnstableApiUsage") // Guava graphs
@CacheableTask
abstract class ResolveExternalDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Resolves external dependencies for single variant."

    if (GradleVersions.isAtLeastGradle74) {
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Cannot serialize Configurations")
    }
  }

  @Transient
  private lateinit var compileClasspath: Configuration

  @Transient
  private lateinit var runtimeClasspath: Configuration

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  abstract val compileFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  abstract val runtimeFiles: ConfigurableFileCollection

  @get:Input
  abstract val projectName: Property<String>

  /** Output in json format for compile classpath graph. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  internal fun configureTask(
    project: Project,
    compileClasspath: Configuration,
    runtimeClasspath: Configuration,
    jarAttr: String
  ) {
    this.compileClasspath = compileClasspath
    this.runtimeClasspath = runtimeClasspath
    compileFiles.setFrom(project.provider { compileClasspath.externalArtifactsFor(jarAttr).artifactFiles })
    runtimeFiles.setFrom(project.provider { runtimeClasspath.externalArtifactsFor(jarAttr).artifactFiles })
    projectName.set(project.name)
  }

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val compileGraph = GraphViewBuilder(compileClasspath, projectName.get()).graph
    val runtimeGraph = GraphViewBuilder(runtimeClasspath, projectName.get()).graph

    val dependencies = compileGraph.nodes().asSequence().plus(runtimeGraph.nodes().asSequence())
      .filterIsInstance<ModuleCoordinates>()
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
  }
}
