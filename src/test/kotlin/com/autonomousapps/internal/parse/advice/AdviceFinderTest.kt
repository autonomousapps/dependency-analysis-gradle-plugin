// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse.advice

import com.autonomousapps.internal.cash.grammar.kotlindsl.model.DependencyDeclaration
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.ProjectType
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AdviceFinderTest {

  @Test
  fun `can find JVM advice`() {
    // Given
    val projectType = ProjectType.JVM
    val caffeineAdvice = Advice.ofRemove(Coordinates.of("com.github.ben-manes.caffeine:caffeine:3.2.3"), "api")
    val okioAdvice = Advice.ofChange(Coordinates.of("com.squareup.okio:okio:3.16.4"), "implementation", "api")
    val advice = setOf(caffeineAdvice, okioAdvice)
    val identityMap: (String) -> String = { it }
    val adviceFinder = AdviceFinder.of(
      projectType = projectType,
      advice = advice,
      reversedDependencyMap = identityMap,
    )
    val okio = DependencyDeclaration(
      configuration = "implementation",
      identifier = DependencyDeclaration.Identifier("\"com.squareup.okio:okio:3.16.4\"", null, false),
      capability = DependencyDeclaration.Capability.DEFAULT,
      type = DependencyDeclaration.Type.MODULE,
      fullText = "implementation(\"com.squareup.okio:okio:3.16.4\")",
    )

    // When
    val actualAdvice = adviceFinder.findAdvice(okio)

    // Then
    assertThat(actualAdvice).isEqualTo(okioAdvice)
  }

  @Nested
  inner class Kmp {
    private val projectType = ProjectType.KMP

    private val caffeineAdvice = Advice.ofRemove(
      Coordinates.of("com.github.ben-manes.caffeine:caffeine:3.2.3"), "jvmMainApi"
    )
    private val okioAdvice = Advice.ofChange(
      Coordinates.of("com.squareup.okio:okio:3.16.4"), "commonMainImplementation", "commonMainApi"
    )
    private val projectAdvice = Advice.ofRemove(Coordinates.of(":producer"), "jvmMainApi")
    private val advice = setOf(caffeineAdvice, okioAdvice, projectAdvice)
    private val identityMap: (String) -> String = { it }
    private val adviceFinder = AdviceFinder.of(
      projectType = projectType,
      advice = advice,
      reversedDependencyMap = identityMap,
    )

    // PLATFORM:   DependencyDeclaration(configuration=api, identifier=project.dependencies.platform("com.squareup.okio:okio-bom:3.16.4"), capability=DEFAULT, type=PROJECT, fullText=api(project.dependencies.platform("com.squareup.okio:okio-bom:3.16.4")), producerConfiguration=null, classifier=null, ext=null, precedingComment=null, isComplex=false)
    // COROUTINES: DependencyDeclaration(configuration=api, identifier="org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2", capability=DEFAULT, type=MODULE, fullText=api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"), producerConfiguration=null, classifier=null, ext=null, precedingComment=null, isComplex=false)
    // OKIO:       DependencyDeclaration(configuration=implementation, identifier="com.squareup.okio:okio:3.16.4", capability=DEFAULT, type=MODULE, fullText=implementation("com.squareup.okio:okio:3.16.4"), producerConfiguration=null, classifier=null, ext=null, precedingComment=null, isComplex=false)
    // CAFFEINE:   DependencyDeclaration(configuration=api, identifier="com.github.ben-manes.caffeine:caffeine:3.2.3", capability=DEFAULT, type=MODULE, fullText=api("com.github.ben-manes.caffeine:caffeine:3.2.3"), producerConfiguration=null, classifier=null, ext=null, precedingComment=null, isComplex=false)
    private val okioDeclaration = DependencyDeclaration(
      configuration = "implementation",
      identifier = DependencyDeclaration.Identifier("\"com.squareup.okio:okio:3.16.4\"", null, false),
      capability = DependencyDeclaration.Capability.DEFAULT,
      type = DependencyDeclaration.Type.MODULE,
      fullText = "implementation(\"com.squareup.okio:okio:3.16.4\")",
    )
    private val okioBomDeclaration = DependencyDeclaration(
      configuration = "api",
      identifier = DependencyDeclaration.Identifier("\"com.squareup.okio:okio-bom:3.16.4\"", null, false),
      capability = DependencyDeclaration.Capability.PLATFORM,
      type = DependencyDeclaration.Type.MODULE,
      fullText = "api(project.dependencies.platform(\"com.squareup.okio:okio-bom:3.16.4\"))",
    )
    private val projectDeclaration = DependencyDeclaration(
      configuration = "api",
      identifier = DependencyDeclaration.Identifier("\":producer\"", null, false),
      capability = DependencyDeclaration.Capability.DEFAULT,
      type = DependencyDeclaration.Type.PROJECT,
      fullText = "api(project(\":producer\"))",
    )

    @Test
    fun `can find KMP module advice`() {
      // Expected
      assertThat(adviceFinder.findAdvice(okioDeclaration, "commonMain")).isEqualTo(okioAdvice)
    }

    @Test
    fun `does not find KMP module platform advice when there isn't any`() {
      // Expected
      assertThat(adviceFinder.findAdvice(okioBomDeclaration, "commonMain")).isNull()
    }

    @Test
    fun `can find KMP project advice`() {
      // Expected
      assertThat(adviceFinder.findAdvice(projectDeclaration, "jvmMain")).isEqualTo(projectAdvice)
    }
  }
}
