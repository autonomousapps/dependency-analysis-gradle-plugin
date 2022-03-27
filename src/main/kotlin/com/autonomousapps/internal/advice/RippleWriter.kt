package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Pebble
import com.autonomousapps.internal.utils.Colors.colorize
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

internal class RippleWriter(pebble: Pebble) {

  private val sourceProject = pebble.sourceProject
  private val ripples = pebble.ripples

  fun buildMessage(): String {
    if (ripples.isEmpty()) {
      return "Project $sourceProject contains no potential ripples."
    }

    val msg = StringBuilder()
    msg.appendReproducibleNewLine("Ripples:")
    ripples
      .groupBy { sourceProject }
      .forEach { (sourceProject, ripplesByProject) ->
        msg.appendReproducibleNewLine(
          "- You have been advised to make a change to ${sourceProject.colorize()} that might impact dependent projects"
        )

        ripplesByProject
          .groupBy { it.downgrade.dependency }
          .forEach { (_, ripplesByDependency) ->
            // subhead text
            val changeText = ripplesByDependency.first().downgrade.changeText()
            msg.appendReproducibleNewLine("  - $changeText")

            // downstream impacts
            ripplesByDependency.forEach { r ->
              val dependentProject = r.impactedProject
              val downstreamTo = r.upgrade.toConfiguration
              msg.appendReproducibleNewLine("    ${dependentProject.colorize()} uses this dependency transitively. You should add it to '$downstreamTo'")
            }
          }
      }
    return msg.toString()
  }

  private fun Advice.changeText(): String {
    return if (toConfiguration == null) "Remove $dependency from '$fromConfiguration'"
    else "Change $dependency from '$fromConfiguration' to '$toConfiguration'"
  }
}
