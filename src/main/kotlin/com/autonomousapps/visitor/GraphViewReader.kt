// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.visitor

import com.autonomousapps.internal.graph.supers.SuperClassGraphBuilder
import com.autonomousapps.internal.graph.supers.SuperNode
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.declaration.internal.Declaration
import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectVariant
import com.google.common.graph.Graph

internal class GraphViewReader(
  private val project: ProjectVariant,
  private val dependencies: Set<Dependency>,
  private val graph: DependencyGraphView,
  private val declarations: Set<Declaration>,
  private val duplicateClasses: Set<DuplicateClass>,
) {

  fun accept(visitor: GraphViewVisitor) {
    val context = DefaultContext(project, dependencies, graph, declarations, duplicateClasses)
    dependencies.forEach { dependency ->
      visitor.visit(dependency, context)
    }
  }
}

internal class DefaultContext(
  override val project: ProjectVariant,
  override val dependencies: Set<Dependency>,
  override val graph: DependencyGraphView,
  override val declarations: Set<Declaration>,
  override val duplicateClasses: Set<DuplicateClass>,
) : GraphViewVisitor.Context {

  // nb: this is a lazy property because it's very expensive to compute, and gated behind a user opt-in.
  override val superGraph: Graph<SuperNode> by unsafeLazy {
    // TODO(tsr): use localClassNames to build smaller graphs? May be necessary to further improve performance of
    //  ComputeUsagesAction::isForMissingSuperclass
    // SuperClassGraphBuilder.of(dependencies, project.classNames)
    SuperClassGraphBuilder.of(dependencies)
  }
}
