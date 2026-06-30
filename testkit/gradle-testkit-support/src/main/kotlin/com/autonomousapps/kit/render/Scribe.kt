// Copyright (c) 2026. Tony Robalik.
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

  /**
   * Invokes [block], appending the requested elements to the internal [buffer], followed by a newline, indenting as
   * appropriate. For example, the following invocation:
   *
   * ```
   * scribe.line { s ->
   *   s.append("foo ")
   *   s.append("bar")
   * }
   * ```
   * Will result in the following string literal:
   * ```
   * "foo bar\n"
   * ```
   */
  public fun line(
    block: (Scribe) -> Unit,
  ): String {
    indent()
    block(this)
    buffer.appendLine()

    return buffer.toString()
  }

  /**
   * This method is *not* indent-aware. If you want an indent, do something like this:
   * ```
   * scribe.line { it.append(obj) }
   * ```
   * @see [line]
   */
  public fun append(obj: Any?): Scribe {
    buffer.append(obj.toString())
    return this
  }

  /**
   * This method is *not* indent-aware. If you want an indent, do something like this:
   * ```
   * scribe.line { it.appendQuoted(obj) }
   * ```
   * @see [line]
   */
  public fun appendQuoted(obj: Any?): Scribe {
    append(quote())
    append(obj.toString())
    append(quote())
    return this
  }

  /**
   * This method is *not* indent-aware. If you want an indent, do something like this:
   * ```
   * scribe.line { it.appendLine() }
   * ```
   * @see [line]
   */
  public fun appendLine(): Scribe {
    buffer.appendLine()
    return this
  }

  private fun indent() {
    buffer.append(" ".repeat(start))
  }

  private fun quote(): String = if (dslKind == GradleProject.DslKind.GROOVY) "'" else "\""
}
