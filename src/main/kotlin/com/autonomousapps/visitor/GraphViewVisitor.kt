// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.visitor

import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.declaration.internal.Declaration

internal interface GraphViewVisitor {
  fun visit(dependency: Dependency, context: Context)

  interface Context {
    val project: ProjectVariant
    val dependencies: Set<Dependency>
    val graph: DependencyGraphView
    val declarations: Set<Declaration>
    val duplicateClasses: Set<DuplicateClass>
  }
}
