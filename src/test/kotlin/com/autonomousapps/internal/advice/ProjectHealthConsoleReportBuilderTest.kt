package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

internal class ProjectHealthConsoleReportBuilderTest {

  @Test
  fun adviceOfRemoveShouldBeSorted() {
    val dependencyAdvice = setOf(
      Advice.ofRemove(ModuleCoordinates("com.project.a", "1.0"), "implementation"),
      Advice.ofRemove(ModuleCoordinates("com.project.c", "1.0"), "api"),
      Advice.ofRemove(ModuleCoordinates("com.project.b", "1.0"), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice).text
    Truth.assertThat(consoleText).isEqualTo("" +
      "Unused dependencies which should be removed:\n" +
      "  api(\"com.project.b:1.0\")\n" +
      "  api(\"com.project.c:1.0\")\n" +
      "  implementation(\"com.project.a:1.0\")" +
      "")
  }

  @Test
  fun adviceOfChangeShouldBeSorted() {
    val dependencyAdvice = setOf(
      Advice.ofChange(ModuleCoordinates("com.project.a", "1.0"), "implementation", "api"),
      Advice.ofChange(ModuleCoordinates("com.project.c", "1.0"), "api", "implementation"),
      Advice.ofChange(ModuleCoordinates("com.project.b", "1.0"), "api", "implementation"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice).text
    Truth.assertThat(consoleText).isEqualTo("" +
      "Existing dependencies which should be modified to be as indicated:\n" +
      "  api(\"com.project.a:1.0\") (was implementation)\n" +
      "  implementation(\"com.project.b:1.0\") (was api)\n" +
      "  implementation(\"com.project.c:1.0\") (was api)" +
      "")
  }

  @Test
  fun adviceOfAddShouldBeSorted() {
    val dependencyAdvice = setOf(
      Advice.ofAdd(ModuleCoordinates("com.project.a", "1.0"), "implementation"),
      Advice.ofAdd(ModuleCoordinates("com.project.c", "1.0"), "api"),
      Advice.ofAdd(ModuleCoordinates("com.project.b", "1.0"), "api"),
    )
    val projectAdvice = ProjectAdvice("dummy", dependencyAdvice, emptySet())

    val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice).text
    Truth.assertThat(consoleText).isEqualTo("" +
      "Transitively used dependencies that should be declared directly as indicated:\n" +
      "  api(\"com.project.b:1.0\")\n" +
      "  api(\"com.project.c:1.0\")\n" +
      "  implementation(\"com.project.a:1.0\")" +
      "")
  }

}
