// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.visitor

import com.autonomousapps.internal.graph.supers.SuperNode
import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.declaration.internal.Declaration
import com.google.common.graph.Graph

internal interface GraphViewVisitor {
  fun visit(dependency: Dependency, context: Context)

  interface Context {
    val project: ProjectVariant
    val dependencies: Set<Dependency>
    val graph: DependencyGraphView
    val declarations: Set<Declaration>
    val duplicateClasses: Set<DuplicateClass>

    /** Graph from child classes up through super classes and interfaces, up to `java/lang/Object`. */
    val superGraph: Graph<SuperNode>
  }
}
