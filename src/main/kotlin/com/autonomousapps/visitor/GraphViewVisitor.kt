package com.autonomousapps.visitor

import com.autonomousapps.model.Dependency
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectVariant
import com.autonomousapps.model.intermediates.Declaration

internal interface GraphViewVisitor {
  fun visit(dependency: Dependency, context: Context)

  interface Context {
    val project: ProjectVariant
    val dependencies: Set<Dependency>
    val graph: DependencyGraphView
    val declarations: Set<Declaration>
  }
}
