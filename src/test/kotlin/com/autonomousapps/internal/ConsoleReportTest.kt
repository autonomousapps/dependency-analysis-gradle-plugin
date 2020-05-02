package com.autonomousapps.internal

import com.autonomousapps.Ignore
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.internal.advice.ComputedAdvice
import com.autonomousapps.internal.advice.filter.FilterSpecBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleReportTest {

  private val orgDotSomething = Dependency("org.some:thing", configurationName = "implementation")
  private val orgDotSomethingElse = Dependency("org.some:thing-else")

  private val orgDotSomethingTrans = TransitiveDependency(orgDotSomething, emptySet())
  private val orgDotSomethingElseTrans = TransitiveDependency(orgDotSomethingElse, emptySet())

  private val orgDotSomethingComponent = ComponentWithTransitives(
    orgDotSomething, mutableSetOf()
  )

  private val somethingComponent = Component(
    dependency = orgDotSomething,
    isTransitive = false,
    isCompileOnlyAnnotations = true,
    classes = emptySet()
  )

  private val annotationProcessor = AnnotationProcessor(orgDotSomething,"fooProcessor", emptySet())
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
    val consoleReport = createReport(addToApiAdvice = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has addToImplAdvice`() {
    // When
    val consoleReport = createReport(addToImplAdvice = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has removeAdvice`() {
    // When
    val consoleReport = createReport(removeAdvice = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has changeToApiAdvice`() {
    // When
    val consoleReport = createReport(changeToApiAdvice = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has compileOnlyDependencies`() {
    // When
    val consoleReport = createReport(compileOnlyDependencies = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `isEmpty should return false when has unusedProcsAdvice`() {
    // When
    val consoleReport = createReport(changeToApiAdvice = setOf(orgDotSomething))

    // Then
    assertFalse(consoleReport.isEmpty(), "Report should not be empty")
  }

  @Test fun `from should return the add advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = setOf(orgDotSomethingTrans),
      undeclaredImplDependencies = setOf(orgDotSomethingElseTrans),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expectedApi = setOf(orgDotSomething)
    val actualApi = consoleReport.addToApiAdvice
    assertEquals(expectedApi, actualApi, "Expected $expectedApi\nActual   $actualApi\n")

    val expectedImpl = setOf(orgDotSomethingElse)
    val actualImpl = consoleReport.addToImplAdvice
    assertEquals(expectedImpl, actualImpl, "Expected $expectedImpl\nActual   $actualImpl\n")
  }

  @Test fun `from should not return the add api advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.usedTransitivesBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = setOf(orgDotSomethingTrans),
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
      unusedComponents = setOf(orgDotSomethingComponent),
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
    val expected = setOf(orgDotSomething)
    val actual = consoleReport.removeAdvice
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun `from should not return the remove advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.unusedDependenciesBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedComponents = setOf(orgDotSomethingComponent),
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
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = setOf(orgDotSomething),
      changeToImpl = setOf(orgDotSomethingElse),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expectedApi = setOf(orgDotSomething)
    val actualApi = consoleReport.changeToApiAdvice
    assertEquals(expectedApi, actualApi, "Expected $expectedApi\nActual   $actualApi\n")

    val expectedImpl = setOf(orgDotSomethingElse)
    val actualImpl = consoleReport.changeToImplAdvice
    assertEquals(expectedImpl, actualImpl, "Expected $expectedImpl\nActual   $actualImpl\n")
  }

  @Test fun `from should not return the change advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.incorrectConfigurationsBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = setOf(orgDotSomething),
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
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = setOf(somethingComponent),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expected = setOf(orgDotSomething)
    val actualApi = consoleReport.compileOnlyDependencies
    assertEquals(expected, actualApi, "Expected $expected\nActual   $actualApi\n")
  }

  @Test fun `from should not return the compileOnly advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.compileOnlyBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = setOf(somethingComponent),
      unusedProcs = emptySet()
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val adviceCompileOnly = consoleReport.compileOnlyDependencies
    assertTrue(adviceCompileOnly.isEmpty(), "Expected $adviceCompileOnly to be empty")
  }

  @Test fun `from should return the unusedProcsAdvice advices from a computed analysis`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = setOf(annotationProcessor)
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val expected = setOf(orgDotSomething)
    val actualUnusedProcsAdvice = consoleReport.unusedProcsAdvice
    assertEquals(expected, actualUnusedProcsAdvice, "Expected $expected\nActual   $actualUnusedProcsAdvice\n")
  }

  @Test fun `from should not return the unusedProcsAdvice advices from a computed analysis with a filter`() {
    // Given
    val filterSpecBuilder = FilterSpecBuilder()
    filterSpecBuilder.unusedProcsBehavior = Ignore
    val computedAdvice = ComputedAdvice(
      unusedComponents = emptySet(),
      undeclaredApiDependencies = emptySet(),
      undeclaredImplDependencies = emptySet(),
      changeToApi = emptySet(),
      changeToImpl = emptySet(),
      filterSpecBuilder = filterSpecBuilder,
      compileOnlyCandidates = emptySet(),
      unusedProcs = setOf(annotationProcessor)
    )

    // When
    val consoleReport = ConsoleReport.from(computedAdvice = computedAdvice)

    // Then
    val adviceUnusedProcsAdvice = consoleReport.unusedProcsAdvice
    assertTrue(adviceUnusedProcsAdvice.isEmpty(), "Expected $adviceUnusedProcsAdvice to be empty")
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
