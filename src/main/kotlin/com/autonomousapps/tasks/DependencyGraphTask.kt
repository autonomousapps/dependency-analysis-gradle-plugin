package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Dependency
import com.autonomousapps.graph.*
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.toIdentifier
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
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
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces the dependency graph, for a given variant, for the current project"
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

    val graph = GraphBuilder(configuration = configuration).buildGraph()

    logger.quiet("Graph JSON at ${outputJsonFile.path}")
    outputJsonFile.writeText(graph.toJson())

    logger.quiet("Graph DOT at ${outputDotFile.path}")
    outputDotFile.writeText(GraphWriter.toDot(graph))
  }
}

private class GraphBuilder(private val configuration: Configuration) {

  private val graph = DependencyGraph()
  private val root = configuration.incoming.resolutionResult.root
  private val nodes = mutableSetOf<Node>()

  /**
   * Returns a [DependencyGraph]. Not a copy, can be mutated.
   */
  fun buildGraph(): DependencyGraph {
    traverseFileDependencies(root, configuration)
    traverse(root, true)
    return graph
  }

  private fun traverseFileDependencies(root: ResolvedComponentResult, configuration: Configuration) {
    val rootDep = root.toDependency()
    val rootNode = ConsumerNode(identifier = rootDep.identifier)

    addNodeOnce(rootNode)

    // the only way to get flat jar file dependencies
    configuration.allDependencies
      .filterIsInstance<FileCollectionDependency>()
      .mapNotNullToSet { it.toIdentifier() }
      .forEach { identifier ->
        val depNode = ProducerNode(identifier = identifier)
        graph.addEdge(Edge(rootNode, depNode))
      }
  }

  private fun traverse(root: ResolvedComponentResult, isConsumer: Boolean = false) {
    val rootDep = root.toDependency()
    // While most nodes are the roots of subgraphs, only one is the absolute root (with in-degree=0)
    val rootNode = if (isConsumer) {
      ConsumerNode(identifier = rootDep.identifier)
    } else {
      ProducerNode(identifier = rootDep.identifier)
    }

    val added = addNodeOnce(rootNode)

    // Don't visit the same node more than once. (Except the root node. We already visited that to
    // find file dependencies earlier, so must visit it again now.)
    if (!added && rootNode !is ConsumerNode) {
      return
    }

    root.dependencies.filterIsInstance<ResolvedDependencyResult>()
      .forEach { dependencyResult ->
        val componentResult = dependencyResult.selected
        val dependency = componentResult.toDependency()

        val depNode = ProducerNode(identifier = dependency.identifier)

        graph.addEdge(Edge(rootNode, depNode))
        traverse(componentResult)
      }
  }

  // Don't visit the same node more than once
  private fun addNodeOnce(node: Node): Boolean {
    if (nodes.contains(node)) {
      return false
    }

    nodes.add(node)
    return true
  }

  private fun ResolvedComponentResult.toDependency(): Dependency =
    when (val componentIdentifier = id) {
      is ProjectComponentIdentifier -> Dependency(componentIdentifier)
      is ModuleComponentIdentifier -> Dependency(componentIdentifier)
      else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
    }
}
