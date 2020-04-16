@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.Component
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.TransitiveComponent
import com.autonomousapps.internal.UnusedDirectComponent
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.stubs.Dependencies
import com.autonomousapps.stubs.Results
import com.autonomousapps.stubs.StubResolvedComponentResult
import com.autonomousapps.stubs.StubResolvedComponentResult.StubProjectComponentIdentifier
import com.autonomousapps.tasks.MisusedDependencyDetector.DependencyReport
import com.autonomousapps.utils.fileFromResource
import org.junit.Test
import kotlin.test.assertTrue

class MisusedDependencyDetectorTest {

  @Test fun `project with dependency issues`() {
    // Given
    val declaredComponents = components()
    val usedClasses = listOf(
      // Brought in transitively at `org.jetbrains:annotations`, from `org.jetbrains.kotlin:kotlin-stdlib`, from `org.jetbrains.kotlin:kotlin-stdlib-jdk7`
      "org.intellij.lang.annotations.Flow",
      // Brought in transitively at `org.jetbrains.kotlin:kotlin-stdlib`, from `org.jetbrains.kotlin:kotlin-stdlib-jdk7`
      "kotlin.Metadata"
    )
    val usedInlineDependencies = emptyList<Dependency>()

    val root = StubResolvedComponentResult(
      dependencies = setOf(Results.javaxInject, Results.kotlinStdlibJdk7),
      componentIdentifier = StubProjectComponentIdentifier(":root")
    )

    // When
    // TODO add test with usedAndroidDependencies not-null
    // TODO add test with usedConstantDependencies not-null
    // TODO add test with manifests not-null
    val actual = MisusedDependencyDetector(
      declaredComponents = declaredComponents,
      usedClasses = usedClasses,
      usedInlineDependencies = usedInlineDependencies,
      usedConstantDependencies = emptyList(),
      manifests = null,
      usedAndroidResBySourceDependencies = null,
      usedAndroidResByResDependencies = null,
      root = root
    ).detect()

    // Then
    val expected = DependencyReport(
      unusedDepsWithTransitives = setOf(
        UnusedDirectComponent(
          Dependencies.javaxInject,
          mutableSetOf()
        ),
        UnusedDirectComponent(
          Dependencies.kotlinStdlibJdk7,
          mutableSetOf(
            // kotlin.Metadata
            Dependencies.kotlinStdlib,
            // `org.intellij.lang.annotations.Flow`
            Dependencies.jetbrainsAnnotations
          )
        )),
      usedTransitives = setOf(
        TransitiveComponent(Dependencies.kotlinStdlib, setOf("kotlin.Metadata")),
        TransitiveComponent(Dependencies.jetbrainsAnnotations, setOf("org.intellij.lang.annotations.Flow"))
      )
    )

    actual expect expected
  }

  private infix fun DependencyReport.expect(expected: DependencyReport) {
    assertTrue("Was      $unusedDepsWithTransitives\nexpected ${expected.unusedDepsWithTransitives}\n") {
      unusedDepsWithTransitives == expected.unusedDepsWithTransitives
    }
    assertTrue("Was      $usedTransitives\nexpected ${expected.usedTransitives}\n") {
      usedTransitives == expected.usedTransitives
    }
  }

  // A collection of direct and transitive components
  private fun components() = fileFromResource("components.json").readText().fromJsonList<Component>()
}


