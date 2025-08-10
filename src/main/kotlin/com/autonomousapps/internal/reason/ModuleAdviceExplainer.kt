// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.reason

import com.autonomousapps.internal.utils.Colors
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.tasks.ReasonTask

internal class ModuleAdviceExplainer(
  private val project: ProjectCoordinates,
  private val unfilteredAndroidScore: AndroidScore?,
  private val finalAndroidScore: AndroidScore?,
) : ReasonTask.Explainer {

  override fun computeReason() = buildString {
    // Header
    appendReproducibleNewLine()
    append(Colors.BOLD)
    appendReproducibleNewLine("-".repeat(40))
    append("You asked about the Android score for '${project.gav()}'.")
    appendReproducibleNewLine(Colors.NORMAL)
    appendReproducibleNewLine(adviceText())
    append(Colors.BOLD)
    append("-".repeat(40))
    appendReproducibleNewLine(Colors.NORMAL)

    finalAndroidScore?.let {
      appendAndroidScoreText(it)
    }
  }.trimEnd()

  private fun adviceText() = buildString {
    when {
      unfilteredAndroidScore == null -> append(
        "There was no Android-related module structure advice for this project, since it is a JVM project."
      )
      finalAndroidScore == null -> append(
        "There was no Android-related module structure advice for this project. It was filtered out."
      )
      finalAndroidScore.shouldBeJvm() -> append(
        "You have been advised to change this project from an Android project to a JVM project. No use of any " +
          "Android feature was detected."
      )
      finalAndroidScore.couldBeJvm() -> append(
        "You have been advised to change this project from an Android project to a JVM project. Only limited use " +
          "of Android feature was detected."
      )
      else -> append(
        "There was no Android-related module structure advice for this project. It uses several Android features."
      )
    }
  }

  private fun StringBuilder.appendAndroidScoreText(finalScore: AndroidScore) {
    if (finalScore.shouldBeJvm()) return

    // score is nonzero, so there are features to print
    appendReproducibleNewLine()
    appendReproducibleNewLine("Android features:")

    with(finalScore) {
      if (usesAndroidClasses) appendReproducibleNewLine("* Uses Android classes.")
      if (hasAndroidRes) appendReproducibleNewLine("* Uses Android resources.")
      if (hasAndroidAssets) appendReproducibleNewLine("* Contains Android assets.")
      if (hasBuildConfig) appendReproducibleNewLine("* Includes BuildConfig.")
      if (hasAndroidDependencies) appendReproducibleNewLine("* Has Android library dependencies.")
      if (hasBuildTypeSourceSplits) appendReproducibleNewLine("* Has non-main source splits.")
    }
  }
}
