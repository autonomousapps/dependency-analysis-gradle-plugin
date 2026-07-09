// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.visitor

import com.autonomousapps.internal.graph.supers.SuperClassGraphBuilder
import com.autonomousapps.internal.graph.supers.SuperNode
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.google.common.graph.Graph

internal class GraphViewReader(
  private val project: ProjectVariant,
  private val dependencies: Set<Dependency>,
  private val graph: DependencyGraphView,
  private val graphRuntime: DependencyGraphView,
  private val declarations: Set<Declaration>,
  private val duplicateClasses: Set<DuplicateClass>,
  private val explodedJarsProvider: () -> Set<ExplodedJar>,
) {

  fun accept(visitor: GraphViewVisitor) {
    val context = DefaultContext(
      project = project,
      dependencies = dependencies,
      graph = graph,
      graphRuntime = graphRuntime,
      declarations = declarations,
      duplicateClasses = duplicateClasses,
      explodedJarsProvider = explodedJarsProvider,
    )
    project.excludedIdentifiers.forEach { excludedIdentifier ->
      visitor.visit(excludedIdentifier)
    }
    dependencies.forEach { dependency ->
      visitor.visit(dependency, context)
    }
  }
}

@Suppress("UnstableApiUsage")
internal class DefaultContext(
  override val project: ProjectVariant,
  override val dependencies: Set<Dependency>,
  override val graph: DependencyGraphView,
  override val graphRuntime: DependencyGraphView,
  override val declarations: Set<Declaration>,
  override val duplicateClasses: Set<DuplicateClass>,
  val explodedJarsProvider: () -> Set<ExplodedJar>,
) : GraphViewVisitor.Context {

  override val binaryClasses: Map<Coordinates, Set<BinaryClass>> by unsafeLazy {
    explodedJarsProvider().associate { jar -> jar.coordinates to jar.binaryClasses }
  }

  // nb: this is a lazy property because it's very expensive to compute, and gated behind a user opt-in.
  override val superGraph: Graph<SuperNode> by unsafeLazy {
    // TODO(tsr): use localClassNames to build smaller graphs? May be necessary to further improve performance of
    //  ComputeUsagesAction::isForMissingSuperclass
    SuperClassGraphBuilder.of(dependencies, binaryClasses)
  }
}
