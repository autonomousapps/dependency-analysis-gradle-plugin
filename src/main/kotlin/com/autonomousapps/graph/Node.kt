package com.autonomousapps.graph

internal sealed class Node(
  open val identifier: String
) : Comparable<Node> {

  override fun toString(): String = identifier

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    if (identifier != (other as? Node)?.identifier) return false

    return true
  }

  override fun hashCode(): Int {
    return identifier.hashCode()
  }

  override fun compareTo(other: Node): Int = identifier.compareTo(other.identifier)
}

internal data class BareNode(override val identifier: String) : Node(identifier)
