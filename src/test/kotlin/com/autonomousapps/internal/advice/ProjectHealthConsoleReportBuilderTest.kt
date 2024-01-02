// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ProjectHealthConsoleReportBuilderTest {

  private val gvi = GradleVariantIdentification.EMPTY

  @Test fun `remove advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofRemove(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation"),
      Advice.ofRemove(ModuleCoordinates("com.project.c", "1.0", gvi), "api"),
      Advice.ofRemove(ModuleCoordinates("com.project.b", "1.0", gvi), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice, DslKind.KOTLIN).text
    assertThat(consoleText).isEqualTo(
      "" +
        "Unused dependencies which should be removed:\n" +
        "  api(\"com.project.b:1.0\")\n" +
        "  api(\"com.project.c:1.0\")\n" +
        "  implementation(\"com.project.a:1.0\")" +
        ""
    )
  }

  @Test fun `change advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofChange(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation", "api"),
      Advice.ofChange(ModuleCoordinates("com.project.c", "1.0", gvi), "api", "implementation"),
      Advice.ofChange(ModuleCoordinates("com.project.b", "1.0", gvi), "api", "implementation"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice, DslKind.KOTLIN).text
    assertThat(consoleText).isEqualTo(
      "" +
        "Existing dependencies which should be modified to be as indicated:\n" +
        "  api(\"com.project.a:1.0\") (was implementation)\n" +
        "  implementation(\"com.project.b:1.0\") (was api)\n" +
        "  implementation(\"com.project.c:1.0\") (was api)" +
        ""
    )
  }

  @Test fun `add advice should be sorted`() {
    val dependencyAdvice = setOf(
      Advice.ofAdd(ModuleCoordinates("com.project.a", "1.0", gvi), "implementation"),
      Advice.ofAdd(ModuleCoordinates("com.project.c", "1.0", gvi), "api"),
      Advice.ofAdd(ModuleCoordinates("com.project.b", "1.0", gvi), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice, DslKind.KOTLIN).text
    assertThat(consoleText).isEqualTo(
      "" +
        "These transitive dependencies should be declared directly:\n" +
        "  api(\"com.project.b:1.0\")\n" +
        "  api(\"com.project.c:1.0\")\n" +
        "  implementation(\"com.project.a:1.0\")" +
        ""
    )
  }
}
