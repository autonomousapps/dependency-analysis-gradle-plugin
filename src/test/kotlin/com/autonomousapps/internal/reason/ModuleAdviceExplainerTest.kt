// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.reason

import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.utils.Colors.decolorize
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ModuleAdviceExplainerTest {

  @Test fun `can explain there is no reason for this to be an Android project`() {
    // Given
    val computer = Fixture().computer()

    // When
    val reason = computer.computeReason()

    // Then
    assertThat(reason.decolorize().lines()).containsExactlyElementsIn(
      """
        
        ----------------------------------------
        You asked about the Android score for ':root'.
        You have been advised to change this project from an Android project to a JVM project. No use of any Android feature was detected.
        ----------------------------------------
      """.trimIndent().lines()
    ).inOrder()
  }

  @Test fun `can explain there is little reason for this to be an Android project`() {
    // Given
    val score = Fixture.emptyScore.copy(
      hasBuildConfig = true,
    )
    val computer = Fixture(
      unfilteredAndroidScore = score,
      finalAndroidScore = score
    ).computer()

    // When
    val reason = computer.computeReason()

    // Then
    assertThat(reason.decolorize().lines()).containsExactlyElementsIn(
      """
        
        ----------------------------------------
        You asked about the Android score for ':root'.
        You have been advised to change this project from an Android project to a JVM project. Only limited use of Android feature was detected.
        ----------------------------------------
        
        Android features:
        * Includes BuildConfig.
      """.trimIndent().lines()
    ).inOrder()
  }

  private class Fixture(
    var unfilteredAndroidScore: AndroidScore = emptyScore,
    var finalAndroidScore: AndroidScore = unfilteredAndroidScore
  ) {

    private val root = ProjectCoordinates(":root", GradleVariantIdentification.EMPTY)

    fun computer() = ModuleAdviceExplainer(
      project = root,
      unfilteredAndroidScore = unfilteredAndroidScore,
      finalAndroidScore = finalAndroidScore
    )

    companion object {
      val emptyScore = AndroidScore(
        hasAndroidAssets = false,
        hasAndroidRes = false,
        usesAndroidClasses = false,
        hasBuildConfig = false,
        hasAndroidDependencies = false,
        hasBuildTypeSourceSplits = false
      )
    }
  }
}
