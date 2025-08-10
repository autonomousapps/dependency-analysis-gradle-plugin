// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.graph

@Suppress("UnstableApiUsage") // Guava
public class DominanceTreeWriter<N : Any>(
  private val root: N,
  private val tree: DominanceTree<N>,
  private val nodeWriter: NodeWriter<N>,
) {

  private val builder = StringBuilder()
  public val string: String get() = builder.toString()

  init {
    compute()
  }

  private fun compute() {
    val visiting = linkedMapOf<N, MutableSet<N>>()

    // start by printing root node
    builder.appendLine(nodeWriter.toString(root))

    fun dfs(node: N) {
      val subs = tree.dominanceGraph.successors(node).run { nodeWriter.comparator()?.let { sortedWith(it) } ?: this }
      visiting[node] = subs.toMutableSet()

      subs.forEach { sub ->
        visiting.forEach { (_, subs) ->
          if (subs.contains(sub)) {
            // SLASH or PLUS
            if (subs.size == 1) {
              builder.append(SLASH)
            } else {
              builder.append(PLUS)
            }
          } else {
            // TAB or PIPE
            if (subs.size == 1) {
              builder.append(TAB)
            } else {
              builder.append(PIPE)
            }
          }
        }

        builder.appendLine(nodeWriter.toString(sub))

        dfs(sub)

        visiting[node]!!.remove(sub)
      }
      visiting.remove(node)
    }

    dfs(root)
  }

  public interface NodeWriter<N : Any> {
    /** A [Comparator] for sorting nodes of type [N]. May be null if you don't care about print order. */
    public fun comparator(): Comparator<N>?

    /** String representation of [node] that will be printed to console. */
    public fun toString(node: N): String

    /** Total size of the node */
    public fun getTreeSize(node: N): Long?

    /** The size of the node itself */
    public fun getSize(node: N): Long?
  }

  private companion object {
    private const val SLASH = """\--- """
    private const val PLUS = "+--- "
    private const val TAB = "     "
    private const val PIPE = "|    "
  }
}
