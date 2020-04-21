package com.autonomousapps.internal

import com.autonomousapps.Ignore
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.advice.ComputedAdvice
import com.autonomousapps.internal.advice.FilterSpecBuilder
import org.junit.Test
import kotlin.test.*

class ConsoleReportTest {

  private val orgDotSomethingV1 = Dependency("org.something", "1.0", "implementation")
  private val orgDotSomethingV2 = Dependency("org.something", "2.0")
  private val componentV1 = Component(
    dependency = orgDotSomethingV1,
    isTransitive = false,
    isCompileOnlyAnnotations = true,
    classes = emptySet()
  )
  /* ****************************************
   * Change advice (incorrect configurations)
   * ****************************************
   */

  @Test fun `isEmpty should return true when empty`() {
    // When
    val consoleReport = createReport()

    // Then
    assertTrue(consoleReport.isEmpty(), "Report should be empty")
  }

  @Test fun `isEmpty should return false when has addToApiAdvice`() {
    // When
    val consoleReport = createReport(addToApiAdvice = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has addToImplAdvice`() {
    // When
    val consoleReport = createReport(addToImplAdvice = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has removeAdvice`() {
    // When
    val consoleReport = createReport(removeAdvice = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has changeToApiAdvice`() {
    // When
    val consoleReport = createReport(changeToApiAdvice = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has compileOnlyDependencies`() {
    // When
    val consoleReport = createReport(compileOnlyDependencies = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has unusedProcsAdvice`() {
    // When
    val consoleReport = createReport(changeToApiAdvice = setOf(orgDotSomethingV1))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `from should return the add advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = setOf(orgDotSomethingV1),
      undeclaredImplDependencies = setOf(orgDotSomethingV2),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expectedApi = setOf(orgDotSomethingV1)
    val actualApi = consoleReport.addToApiAdvice
    assertEquals(expectedApi, actualApi, "Expected $expectedApi\nActual   $actualApi\n")

    val expectedImpl = setOf(orgDotSomethingV2)
    val actualImpl = consoleReport.addToImplAdvice
    assertEquals(expectedImpl, actualImpl, "Expected $expectedImpl\nActual   $actualImpl\n")
  }

  @Test fun `from should not return the add api advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.usedTransitivesBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = setOf(orgDotSomethingV1),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val advice = consoleReport.addToApiAdvice
    assertTrue(advice.isEmpty(), "Expected $advice to be empty")
  }

  @Test fun `from should return the remove api advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedDependencies = setOf(orgDotSomethingV1),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expected = setOf(orgDotSomethingV1)
    val actual = consoleReport.removeAdvice
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun `from should not return the remove advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.unusedDependenciesBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedDependencies = setOf(orgDotSomethingV1),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val advice = consoleReport.addToApiAdvice
    assertTrue(advice.isEmpty(), "Expected $advice to be empty")
  }

  @Test fun `from should return the change advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = setOf(orgDotSomethingV1),
      changeToImpl = setOf(orgDotSomethingV2),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expectedApi = setOf(orgDotSomethingV1)
    val actualApi = consoleReport.changeToApiAdvice
    assertEquals(expectedApi, actualApi, "Expected $expectedApi\nActual   $actualApi\n")

    val expectedImpl = setOf(orgDotSomethingV2)
    val actualImpl = consoleReport.changeToImplAdvice
    assertEquals(expectedImpl, actualImpl, "Expected $expectedImpl\nActual   $actualImpl\n")
  }

  @Test fun `from should not return the change advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.incorrectConfigurationsBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = setOf(orgDotSomethingV1),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val adviceApi = consoleReport.changeToApiAdvice
    assertTrue(adviceApi.isEmpty(), "Expected $adviceApi to be empty")
    val adviceImpl = consoleReport.changeToImplAdvice
    assertTrue(adviceImpl.isEmpty(), "Expected $adviceImpl to be empty")
  }

  @Test fun `from should return the compileOnly advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = setOf(componentV1),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expected = setOf(orgDotSomethingV1)
    val actualApi = consoleReport.compileOnlyDependencies
    assertEquals(expected, actualApi, "Expected $expected\nActual   $actualApi\n")
  }

  @Test fun `from should not return the compileOnly advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.compileOnlyBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedDependencies = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = setOf(componentV1),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val adviceApi = consoleReport.changeToApiAdvice
    assertTrue(adviceApi.isEmpty(), "Expected $adviceApi to be empty")
    val adviceImpl = consoleReport.changeToImplAdvice
    assertTrue(adviceImpl.isEmpty(), "Expected $adviceImpl to be empty")
  }

  private fun createReport(
    addToApiAdvice: Set<Dependency> = emptySet(),
    addToImplAdvice: Set<Dependency> = emptySet(),
    removeAdvice: Set<Dependency> = emptySet(),
    changeToApiAdvice: Set<Dependency> = emptySet(),
    changeToImplAdvice: Set<Dependency> = emptySet(),
    compileOnlyDependencies: Set<Dependency> = emptySet(),
    unusedProcsAdvice: Set<Dependency> = emptySet()
  ) = ConsoleReport(
    addToApiAdvice,
    addToImplAdvice,
    removeAdvice,
    changeToApiAdvice,
    changeToImplAdvice,
    compileOnlyDependencies,
    unusedProcsAdvice
  )
}
