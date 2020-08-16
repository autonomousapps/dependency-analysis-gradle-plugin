package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Dependency
import com.autonomousapps.graph.*
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class DependencyGraphTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces the dependency graph for the current project"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   * It is the result of resolving `runtimeClasspath`. cf. [configuration]
   */
  @get:Classpath
  abstract val artifactFiles: ConfigurableFileCollection

  /**
   * This is what the task actually uses as its input. We really only care about the
   * [ResolutionResult]. cf. [artifactFiles].
   */
  @get:Internal
  lateinit var configuration: Configuration

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    val outputJsonFile = outputJson.getAndDelete()
    val outputDotFile = outputDot.getAndDelete()

    val graph = GraphBuilder(
      root = configuration.incoming.resolutionResult.root
    ).buildGraph()

    logger.quiet("Graph JSON at ${outputJsonFile.path}")
    outputJsonFile.writeText(graph.toJson())

    logger.quiet("Graph DOT at ${outputDotFile.path}")
    outputDotFile.writeText(GraphWriter.toDot(graph))
  }
}

private class GraphBuilder(
  private val root: ResolvedComponentResult
) {

  private val graph = DependencyGraph()
  private val nodes = mutableListOf<Node>()

  /**
   * Returns a [DependencyGraph]. Not a copy, can be mutated.
   */
  fun buildGraph(): DependencyGraph {
    traverse(root, true)
    return graph
  }

  private fun traverse(root: ResolvedComponentResult, isConsumer: Boolean = false) {
    val rootDep = root.toDependency()
    // While most nodes are the roots of subgraphs, only one is the absolute root (with in-degree=0)
    val rootNode = if (isConsumer) {
      ConsumerNode(identifier = rootDep.identifier)
    } else {
      ProducerNode(identifier = rootDep.identifier)
    }

    // Don't visit the same node more than once
    if (nodes.contains(rootNode)) {
      return
    }
    nodes.add(rootNode)

    root.dependencies.filterIsInstance<ResolvedDependencyResult>()
      .map { dependencyResult ->
        val componentResult = dependencyResult.selected
        val dependency = componentResult.toDependency()

        val depNode = ProducerNode(identifier = dependency.identifier)

        graph.addEdge(Edge(rootNode, depNode))
        traverse(componentResult)
      }
  }

  private fun ResolvedComponentResult.toDependency(): Dependency =
    when (val componentIdentifier = id) {
      is ProjectComponentIdentifier -> Dependency(componentIdentifier)
      is ModuleComponentIdentifier -> Dependency(componentIdentifier)
      else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
    }
}
