package com.autonomousapps.internal.graph.supers

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.visitor.GraphViewVisitor
import com.google.common.graph.Graph

/**
 * This calculation is very expensive, so we cache it.
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1358">Issue 1358</a>
 */
@Suppress("UnstableApiUsage") // Guava graphs
internal class SuperClassGraphHolder(private val context: GraphViewVisitor.Context) {

  /** Graph from child classes up through super classes and interfaces, up to `java/lang/Object`. */
  val superGraph: Graph<SuperNode> by unsafeLazy { SuperClassGraphBuilder.of(context) }

  /** The set of super classes and interfaces not available from [context] (therefore "external" to "this" module). */
  val externals: Set<String> by unsafeLazy { computeExternalSupers() }

  private fun computeExternalSupers(): Set<String> {
    val supers = context.project.codeSource.mapNotNullToOrderedSet { src -> src.superClass }
    val interfaces = context.project.codeSource.flatMapToOrderedSet { src -> src.interfaces }
    // These are all the super classes and interfaces in "this" module
    val localClasses = context.project.codeSource.mapToOrderedSet { src -> src.className }
    // These super classes and interfaces are not available from "this" module, so must come from dependencies.
    val externalSupers = supers - localClasses
    val externalInterfaces = interfaces - localClasses
    val externals = externalSupers + externalInterfaces

    return externals
  }
}
