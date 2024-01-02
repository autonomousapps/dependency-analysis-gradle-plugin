// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.graph

import com.google.common.graph.ElementOrder
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder

/**
 * Used to compute a dominance tree from [backingGraph] rooted on [root]. Note that the algorithm is supposed to be general to
 * directed graphs, but has only been tested against acyclic directed graphs with a single entry point (i.e., dependency
 * graphs).
 *
 * @see <a href="http://www.hipersoft.rice.edu/grads/publications/dom14.pdf">A Simple, Fast Dominance Algorithm.</a>
 */
@Suppress("UnstableApiUsage") // Guava graphs
public class DominanceTree<N : Any>(
  private val backingGraph: Graph<N>,
  private val root: N
) {

  // map of N to its IDom (immediate dominator)
  private val backingNodes = backingGraph.nodes()
  private val doms = HashMap<N, N>(backingNodes.size).apply {
    // initialize map. Each node is its own dominator
    backingNodes.forEach {
      put(it, it)
    }
  }

  // TODO probably avoid use of Topological, because it gives the topological order, which is actually the opposite of
  //  what we need for this algorithm, and is why we have `top.size - i` below, rather than just `i`.
  // For the reverse postorder
  private val top = Topological(backingGraph, root).order.toList()

  // This algorithm requires access to the node's "number" in the topological (reverse post) order
  private val nodeNumberMap: Map<N, Int> = top.mapIndexed { i, n -> n to (top.size - i) }.toMap()
  private val N.nodeNumber: Int get() = nodeNumberMap[this]!!

  init {
    computeDominance()
  }

  private fun computeDominance() {
    var changed = true
    while (changed) {
      changed = false

      // for all nodes, b, in reverse postorder (except start node)
      top.asSequence()
        .filterNot { it == root }
        .forEach { n ->
          // new idom ← first (processed) predecessor of b (pick one)
          val predecessors = backingGraph.predecessors(n)
          var newIDom = predecessors.first()

          (predecessors - newIDom).forEach { p ->
            // i.e., if doms[p] already calculated
            if (doms[p] != p) {
              newIDom = intersect(p, newIDom)
            }
          }

          if (doms[n] != newIDom) {
            doms[n] = newIDom
            changed = true
          }
        }
    }
  }

  private fun intersect(n1: N, n2: N): N {
    var left = n1
    var right = n2
    while (left.nodeNumber != right.nodeNumber) {
      while (left.nodeNumber < right.nodeNumber) left = doms[left]!!
      while (right.nodeNumber < left.nodeNumber) right = doms[right]!!
    }
    return left
  }

  public val dominanceGraph: Graph<N> by lazy(LazyThreadSafetyMode.NONE) {
    val builder = GraphBuilder.directed()
      .allowsSelfLoops(false)
      .incidentEdgeOrder(ElementOrder.stable<N>())
      .immutable<N>()

    // edges
    doms.mapNotNull { (sub, dom) ->
      if (sub != dom) dom to sub else null
    }.forEach { builder.putEdge(it.first, it.second) }

    builder.build()
  }
}

/*

  Description of algorithm from p6 of Rice paper:

 for all nodes, n /* initialize the dominators array */
   doms[n] ← Undefined
 doms[start node] ← start node
 Changed ← true
 while (Changed)
   Changed ← false
   for all nodes, n, in reverse postorder (except start node)
     new idom ← first (processed) predecessor of n /* (pick one) */
     for all other predecessors, p, of n
       if doms[p] != Undefined /* i.e., if doms[p] already calculated */
         new idom ← intersect(p, new idom)
     if doms[n] != new idom
       doms[n] ← new idom
       Changed ← true

 function intersect(left, right) returns node
   finger1 ← left
   finger2 ← right
   while (finger1 != finger2)
     while (finger1 < finger2)
       finger1 = doms[finger1]
     while (finger2 < finger1)
       finger2 = doms[finger2]
   return finger1

 */
