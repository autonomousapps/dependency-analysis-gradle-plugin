package com.autonomousapps.kit.render

import com.autonomousapps.kit.GradleProject

class Scribe @JvmOverloads constructor(
  /** Which Gradle DSL to use for rendering. */
  val dslKind: GradleProject.DslKind = GradleProject.DslKind.GROOVY,

  /** Indent level when entering a block. */
  val indent: Int = 2,
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

  internal fun block(
    element: Element.Block,
    block: (Scribe) -> Unit,
  ): String {
    // e.g., "plugins {"
    indent()
    buffer.append(element.name)
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

  internal fun line(
    block: (Scribe) -> Unit,
  ): String {
    indent()
    block(this)
    buffer.appendLine()

    return buffer.toString()
  }

  internal fun append(obj: Any?) {
    buffer.append(obj.toString())
  }

  private fun indent() {
    buffer.append(" ".repeat(start))
  }
}
