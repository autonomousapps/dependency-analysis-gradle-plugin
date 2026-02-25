// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.render

import com.autonomousapps.kit.GradleProject

public class Scribe @JvmOverloads constructor(
  /** Which Gradle DSL to use for rendering. */
  public val dslKind: GradleProject.DslKind = GradleProject.DslKind.GROOVY,

  /** Indent level when entering a block. */
  public val indent: Int = 2,
) : AutoCloseable {

  private val buffer = StringBuilder()

  /** Starting indent for any block. */
  private var start: Int = 0

  /** Enter a block, increase the indent. */
  private fun enter() {
    start += indent
  }

  /** Exit a block, decrease the indent. */
  private fun exit() {
    start -= indent
  }

  override fun close() {
    buffer.clear()
    start = 0
  }

  public fun block(
    element: Element.Block,
    block: (Scribe) -> Unit,
  ): String = block(element.name, block)

  public fun block(
    name: String,
    block: (Scribe) -> Unit,
  ): String {
    // e.g., "plugins {"
    indent()
    buffer.append(name)
    buffer.appendLine(" {")

    // increase the indent
    enter()

    // write the block inside the {}
    block(this)

    // decrease the indent
    exit()

    // closing brace
    indent()
    buffer.appendLine("}")

    // return the string
    return buffer.toString()
  }

  public fun line(
    block: (Scribe) -> Unit,
  ): String {
    indent()
    block(this)
    buffer.appendLine()

    return buffer.toString()
  }

  public fun append(obj: Any?): Scribe {
    buffer.append(obj.toString())
    return this
  }

  public fun appendQuoted(obj: Any?): Scribe {
    append(quote())
    append(obj.toString())
    append(quote())
    return this
  }

  public fun appendLine(): Scribe {
    buffer.appendLine()
    return this
  }

  private fun indent() {
    buffer.append(" ".repeat(start))
  }

  private fun quote(): String = if (dslKind == GradleProject.DslKind.GROOVY) "'" else "\""
}
