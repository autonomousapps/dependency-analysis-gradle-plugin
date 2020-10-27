package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Ripple
import com.autonomousapps.advice.UpstreamRipple
import com.autonomousapps.internal.utils.fromJsonList
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

abstract class RipplesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Emits to console all potential 'ripples' relating to dependency advice"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val ripples: RegularFileProperty

  @TaskAction fun action() {
    val ripples = ripples.fromJsonList<Ripple>()

    if (ripples.isEmpty()) {
      logger.quiet("Your project contains no potential ripples.")
      return
    }

    logger.quiet("Ripples:")
    ripples.groupBy { it.upstreamRipple.projectPath }.forEach { (dependencyProject, r) ->
      val msg = StringBuilder("- You have been advised to make a change to $dependencyProject that might impact dependent projects\n")

      r.forEach { ripple ->
        val dependentProject = ripple.downstreamImpact.projectPath
        val changeText = ripple.upstreamRipple.changeText()
        val downstreamTo = ripple.downstreamImpact.toConfiguration
        msg.appendReproducibleNewLine("  - $changeText") // TODO this line does not need to be repeated. Should do another grouping on ripple.upstreamRipple.providedDependency
          .appendReproducibleNewLine("    $dependentProject uses this dependency transitively. You should add it to '$downstreamTo'")
      }
      logger.quiet(msg.toString())
    }
  }

  private fun UpstreamRipple.changeText(): String =
    if (toConfiguration == null) "Remove $providedDependency from '$fromConfiguration'"
    else "Change $providedDependency from '$fromConfiguration' to '$toConfiguration'"
}
