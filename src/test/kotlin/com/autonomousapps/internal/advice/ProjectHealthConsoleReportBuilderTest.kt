// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.utils.Colors.decolorize
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ProjectHealthConsoleReportBuilderTest {

  private val gvi = GradleVariantIdentification.EMPTY
  private val postscript = "For help understanding this report, please ask in #my-cool-slack-channel"

  @Test fun `remove advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofRemove(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation"),
      Advice.ofRemove(ModuleCoordinates("com.project.c", "1.0", gvi), "api"),
      Advice.ofRemove(ModuleCoordinates("com.project.b", "1.0", gvi), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(
      projectAdvice = projectAdvice,
      postscript = postscript,
      dslKind = DslKind.KOTLIN,
    ).text
    assertThat(consoleText.decolorize()).isEqualTo(
      """
        Unused dependencies which should be removed:
          api("com.project.b:1.0")
          api("com.project.c:1.0")
          implementation("com.project.a:1.0")
        
        For help understanding this report, please ask in #my-cool-slack-channel
      """.trimIndent()
    )
  }

  @Test fun `change advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofChange(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation", "api"),
      Advice.ofChange(ModuleCoordinates("com.project.c", "1.0", gvi), "api", "implementation"),
      Advice.ofChange(ModuleCoordinates("com.project.b", "1.0", gvi), "api", "implementation"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(
      projectAdvice = projectAdvice,
      postscript = postscript,
      dslKind = DslKind.KOTLIN
    ).text
    assertThat(consoleText.decolorize()).isEqualTo(
      """
        Existing dependencies which should be modified to be as indicated:
          api("com.project.a:1.0") (was implementation)
          implementation("com.project.b:1.0") (was api)
          implementation("com.project.c:1.0") (was api)
        
        For help understanding this report, please ask in #my-cool-slack-channel
      """.trimIndent()
    )
  }

  @Test fun `add advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofAdd(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation"),
      Advice.ofAdd(ModuleCoordinates("com.project.c", "1.0", gvi), "api"),
      Advice.ofAdd(ModuleCoordinates("com.project.b", "1.0", gvi), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(
      projectAdvice = projectAdvice,
      postscript = postscript,
      dslKind = DslKind.KOTLIN,
    ).text
    assertThat(consoleText.decolorize()).isEqualTo(
      """
        These transitive dependencies should be declared directly:
          api("com.project.b:1.0")
          api("com.project.c:1.0")
          implementation("com.project.a:1.0")
        
        For help understanding this report, please ask in #my-cool-slack-channel
      """.trimIndent()
    )
  }
}
