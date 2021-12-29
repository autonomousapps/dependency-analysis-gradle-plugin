package com.autonomousapps.visitor

import com.autonomousapps.model.Dependency
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectVariant
import com.autonomousapps.model.intermediates.Declaration

internal class GraphViewReader(
  private val project: ProjectVariant,
  private val dependencies: Set<Dependency>,
  private val graph: DependencyGraphView,
  private val declarations: Set<Declaration>
) {

  fun accept(visitor: GraphViewVisitor) {
    val context = DefaultContext(project, dependencies, graph, declarations)
    dependencies.forEach { dependency ->
      visitor.visit(dependency, context)
    }
  }
}

internal class DefaultContext(
  override val project: ProjectVariant,
  override val dependencies: Set<Dependency>,
  override val graph: DependencyGraphView,
  override val declarations: Set<Declaration>
) : GraphViewVisitor.Context
