package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.isJavaPlatform
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Variant
import com.google.common.graph.Graph
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

@CacheableTask
abstract class GraphViewTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Constructs a variant-specific view of this project's dependency graph"
  }

  private lateinit var compileClasspath: Configuration

  fun setCompileClasspath(compileClasspath: Configuration) {
    this.compileClasspath = compileClasspath
  }

  @get:Internal
  abstract val jarAttr: Property<String>

  @Classpath
  fun getCompileClasspath(): FileCollection = compileClasspath
    .artifactsFor(jarAttr.get())
    .artifactFiles

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

/**
 * Walks the resolved dependency graph to create a dependency graph rooted on the current project.
 */
@Suppress("UnstableApiUsage") // Guava Graph
private class GraphViewBuilder(conf: Configuration) {

  val graph: Graph<Coordinates>

  private val graphBuilder = DependencyGraphView.newGraphBuilder()

  private val visited = mutableSetOf<Coordinates>()

  init {
    val root = conf
      .incoming
      .resolutionResult
      .root

    walkFileDeps(root, conf)
    walk(root)

    graph = graphBuilder.build()
  }

  private fun walkFileDeps(root: ResolvedComponentResult, conf: Configuration) {
    val rootId = root.id.toCoordinates()
    graphBuilder.addNode(rootId)

    // the only way to get flat jar file dependencies
    conf.allDependencies
      .filterIsInstance<FileCollectionDependency>()
      .mapNotNullToSet { it.toCoordinates() }
      .forEach { id ->
        graphBuilder.putEdge(rootId, id)
      }
  }

  private fun walk(root: ResolvedComponentResult) {
    val rootId = root.id.toCoordinates()

    root.dependencies
      .filterIsInstance<ResolvedDependencyResult>()
      // AGP adds all runtime dependencies as constraints to the compile classpath, and these show
      // up in the resolution result. Filter them out.
      .filterNot { it.isConstraint }
      // For similar reasons as above
      .filterNot { it.isJavaPlatform() }
      // Sometimes there is a self-dependency?
      .filterNot { it.selected == root }
      .forEach { dependencyResult ->
        val depId = dependencyResult.selected.id.toCoordinates()

        // add an edge
        graphBuilder.putEdge(rootId, depId)

        if (!visited.contains(depId)) {
          visited.add(depId)
          // recursively walk the graph in a depth-first pattern
          walk(dependencyResult.selected)
        }
      }
  }
}

// TODO move
@Suppress("UnstableApiUsage")
internal object GraphWriter {

  fun toDot(graph: Graph<Coordinates>) = buildString {
    val projectNodes = graph.nodes()
      .filterIsInstance<ProjectCoordinates>()
      .map { it.gav() }

    appendReproducibleNewLine("strict digraph DependencyGraph {")
    appendReproducibleNewLine("  ratio=0.6;")
    appendReproducibleNewLine("  node [shape=box];")
    projectNodes.forEach {
      appendReproducibleNewLine("\n  \"$it\" [style=filled fillcolor=\"#008080\"];")
    }

    graph.edges().forEach { edge ->
      val source = edge.nodeU()
      val target = edge.nodeV()
      val style =
        if (source is ProjectCoordinates && target is ProjectCoordinates) " [style=bold color=\"#FF6347\" weight=8]"
        else ""
      append("  \"${source.gav()}\" -> \"${target.gav()}\"$style;")
      append("\n")
    }
    append("}")
  }
}
