package com.autonomousapps.internal.graph

import com.autonomousapps.internal.isJavaPlatform
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.graph.Graph
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/** Walks the resolved dependency graph to create a dependency graph rooted on the current project. */
@Suppress("UnstableApiUsage") // Guava Graph
internal class GraphViewBuilder(
  private val rootId: ProjectCoordinates,
  root: ResolvedComponentResult,
  private val fileDeps: List<String>,
) {

//  constructor()

  val graph: Graph<Coordinates>

  private val graphBuilder = DependencyGraphView.newGraphBuilder()

  private val visited = mutableSetOf<Coordinates>()

  init {
    graphBuilder.addNode(rootId)

    walkFileDeps()
    walk(root, rootId)

    graph = graphBuilder.build()
  }

  private fun walkFileDeps() {
    fileDeps
      .map { Coordinates.of(it) }
      .forEach { id ->
        graphBuilder.putEdge(rootId, id)
      }
  }

  private fun walk(root: ResolvedComponentResult, rootId: Coordinates) {
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
        // Might be from an included build, in which case the coordinates reflect the _requested_ dependency instead of
        // the _resolved_ dependency.
        val depId = dependencyResult.toCoordinates()

        // add an edge
        graphBuilder.putEdge(rootId, depId)

        if (!visited.contains(depId)) {
          visited.add(depId)
          // recursively walk the graph in a depth-first pattern
          walk(dependencyResult.selected, depId)
        }
      }
  }
}

/** Instances of this time should be annotated with `@Nested`. */
abstract class GraphInput(
  @get:Input val classpathName: Property<String>,
  @get:Input val fileDeps: ListProperty<String>,
  @get:Input val graph: Property<ResolvedComponentResult>
)
