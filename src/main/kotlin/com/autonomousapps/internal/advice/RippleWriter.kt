package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Ripple
import com.autonomousapps.advice.UpstreamSource
import com.autonomousapps.internal.utils.colorize
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

internal class RippleWriter(
  private val ripples: List<Ripple>
) {

  fun buildMessage(): String {
    if (ripples.isEmpty()) {
      return "Your project contains no potential ripples."
    }

    val msg = StringBuilder()
    msg.appendReproducibleNewLine("Ripples:")
    ripples.groupBy { it.upstreamSource.projectPath }
      .forEach { (dependencyProject, ripplesByProject) ->
        msg.appendReproducibleNewLine("- You have been advised to make a change to ${dependencyProject.colorize()} that might impact dependent projects")

        ripplesByProject.groupBy { it.upstreamSource.providedDependency }
          .forEach { (_, ripplesByDependency) ->
            // subhead text
            val changeText = ripplesByDependency.first().upstreamSource.changeText()
            msg.appendReproducibleNewLine("  - $changeText")

            // downstream impacts
            ripplesByDependency.forEach { r ->
              val dependentProject = r.downstreamImpact.impactProjectPath
              val downstreamTo = r.downstreamImpact.toConfiguration
              msg.appendReproducibleNewLine("    ${dependentProject.colorize()} uses this dependency transitively. You should add it to '$downstreamTo'")
            }
          }
      }
    return msg.toString()
  }

  private fun UpstreamSource.changeText(): String =
    if (toConfiguration == null) "Remove $providedDependency from '$fromConfiguration'"
    else "Change $providedDependency from '$fromConfiguration' to '$toConfiguration'"
}
