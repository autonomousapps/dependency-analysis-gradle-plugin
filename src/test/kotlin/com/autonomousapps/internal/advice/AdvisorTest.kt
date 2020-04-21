package com.autonomousapps.internal.advice

import com.autonomousapps.Ignore
import com.autonomousapps.Warn
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.tasks.AdviceTask
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdvisorTest {

  private val project1 = Dependency(":project1")
  private val project2 = Dependency(":project2")
  private val project3 = Dependency(":project3")

  private val library1 = Dependency("some:lib1", "1")
  private val library2 = Dependency("some:lib2", "1")
  private val library3 = Dependency("some:lib3", "1")

  private val library4 = Dependency("some:lib4", "1", "api")
  private val library5 = Dependency("some:lib5", "1", "api")
  private val library6 = Dependency("some:lib6", "1", "implementation")

  /* ****************************************
   * Change advice (incorrect configurations)
   * ****************************************
   */

  @Test fun changeAdvice() {
    // Given
    // 4 (api), 5 (api), and 6 (impl) are declared. 3, 4, and 6 are abi. Therefore 6 should be "changed" from impl to
    // api, 5 should be changed from api to implementation, and 3 from null to api.
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library5, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute()
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNotNull(changeAdvice, "Change advice should not be null")

    val expected = sortedSetOf(
      Advice.change(library5, "implementation"),
      Advice.change(library6, "api")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isChange() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun changeAdviceRespectsIgnoreChangeRule() {
    // Given
    // 4 (api), 5 (api), and 6 (impl) are declared. 3 is undeclared but should be api. 5 is api but should be impl. 6 is
    // impl but should be api.
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library5, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { incorrectConfigurationsBehavior = Ignore })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNull(changeAdvice, "Change advice should be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isChange() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun changeAdviceRespectsIgnoreAllRule() {
    // Given
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library5, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Ignore })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNull(changeAdvice, "Change advice should be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices()
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun changeAdviceRespectsIgnoreSomeFromSpecificRule() {
    // Given
    // 4 (api), 5 (api), and 6 (impl) are declared. 3, 4, and 6 are ABI. 3: null -> api. 4: no change. 5: api -> impl.
    // 6: impl -> api
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library5, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply {
      incorrectConfigurationsBehavior = Warn(setOf(library5.identifier))
    })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNotNull(changeAdvice, "Change advice should not be null")

    val expected = sortedSetOf(Advice.change(library6, "api"))
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isChange() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun changeAdviceRespectsIgnoreSomeFromAnyRule() {
    // Given
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library5, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Warn(setOf(library5.identifier)) })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNotNull(changeAdvice, "Change advice should not be null")

    val expected = sortedSetOf(Advice.change(library6, "api"))
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isChange() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun changeAdviceDoesNotAdviseChangingCompileOnlyDependency() {
    // Given
    val allComponents = listOf(
      Component(dependency = library3, isTransitive = true, isCompileOnlyAnnotations = false, classes = emptySet()),
      Component(dependency = library4, isTransitive = false, isCompileOnlyAnnotations = false, classes = emptySet()),
      // Because this component is a compileOnly candidate, it will not show up in the change advice.
      Component(dependency = library6, isTransitive = false, isCompileOnlyAnnotations = true, classes = emptySet())
    )
    val abiDeps = listOf(library3, library4, library6)
    val allDeclaredDeps = listOf(library4, library6)
    // a precondition to ensure I don't set up the test incorrectly
    assertTrue("No declared deps will have a null configuration") {
      allDeclaredDeps.none { it.configurationName == null }
    }

    // When
    val advisor = Advisor(
      allComponents = allComponents,
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = emptyList(),
      abiDeps = abiDeps,
      allDeclaredDeps = allDeclaredDeps,
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder())
    var consoleReport = ConsoleReport.from(computedAdvice)
    val changeAdvice = AdvicePrinter(consoleReport).getChangeAdvice()

    // Then
    assertNull(changeAdvice, "There should be nothing to change")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isChange() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  /* *****************************************
   * Add advice (used transitive dependencies)
   * *****************************************
   */

  @Test fun addAdvice() {
    // Given
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder())
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNotNull(addAdvice, "Add advice should not be null")

    val expected = sortedSetOf(
      Advice.add(project1, "api"),
      Advice.add(project2, "implementation"),
      Advice.add(project3, "api")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceRespectsIgnoreChangeRule() {
    // Given
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { usedTransitivesBehavior = Ignore })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNull(addAdvice, "Add advice should be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceRespectsIgnoreAllRule() {
    // Given
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Ignore })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNull(addAdvice, "Add advice should not be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceRespectsIgnoreSomeFromSpecificRule() {
    // Given
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply {
      usedTransitivesBehavior = Warn(setOf(project1.identifier))
    })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNotNull(addAdvice, "Add advice should not be null")

    val expected = sortedSetOf(
      Advice.add(project2, "implementation"),
      Advice.add(project3, "api")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceRespectsIgnoreSomeFromAnyRule() {
    // Given
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Warn(setOf(project1.identifier)) })
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNotNull(addAdvice, "Add advice should not be null")

    val expected = sortedSetOf(
      Advice.add(project2, "implementation"),
      Advice.add(project3, "api")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceDoesNotAdviseAddingCompileOnlyDependency() {
    // Given
    val allComponents = listOf(
      Component(project1, isTransitive = true, isCompileOnlyAnnotations = false, classes = emptySet()),
      Component(project2, isTransitive = true, isCompileOnlyAnnotations = false, classes = emptySet()),
      Component(project3, isTransitive = false, isCompileOnlyAnnotations = true, classes = emptySet())
    )
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1, project3)

    // When
    val advisor = Advisor(
      allComponents = allComponents,
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder())
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNotNull(addAdvice, "Add advice should not be null")

    val expected = sortedSetOf(
      Advice.add(project1, "api"),
      Advice.add(project2, "implementation")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun addAdviceDoesNotAdviseAddingTransitiveCompileOnlyDependency() {
    // Given
    val allComponents = listOf(
      Component(project1, isTransitive = true, isCompileOnlyAnnotations = false, classes = emptySet()),
      Component(project2, isTransitive = true, isCompileOnlyAnnotations = true, classes = emptySet())
    )
    val usedTransitiveComponents = listOf(
      TransitiveComponent(project1, emptySet()),
      TransitiveComponent(project2, emptySet())
    )
    val abiDeps = listOf(project1)

    // When
    val advisor = Advisor(
      allComponents = allComponents,
      unusedDirectComponents = emptyList(),
      usedTransitiveComponents = usedTransitiveComponents,
      abiDeps = abiDeps,
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute()
    var consoleReport = ConsoleReport.from(computedAdvice)
    val addAdvice = AdvicePrinter(consoleReport).getAddAdvice()

    // Then
    assertNotNull(addAdvice, "Add advice should not be null")

    val expected = sortedSetOf(
      Advice.add(project1, "api")
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isAdd() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  /* ***********************************
   * Remove advice (unused dependencies)
   * ***********************************
   */

  @Test fun removeAdvice() {
    // Given
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf(library1)),
      UnusedDirectComponent(project2, mutableSetOf(library2)),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute()

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNotNull(removeAdvice, "Remove advice should not be null")

    val expected = sortedSetOf(
      Advice.remove(project1),
      Advice.remove(project2),
      Advice.remove(project3)
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun removeAdviceRespectsIgnoreChangeRule() {
    // Given
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf(library1)),
      UnusedDirectComponent(project2, mutableSetOf(library2)),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { unusedDependenciesBehavior = Ignore })

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNull(removeAdvice, "Remove advice should be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun removeAdviceRespectsIgnoreAllRule() {
    // Given
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf(library1)),
      UnusedDirectComponent(project2, mutableSetOf(library2)),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Ignore })

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNull(removeAdvice, "Remove advice should be null")

    val expected = emptySet<Advice>()
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun removeAdviceRespectsIgnoreSomeFromSpecificRule() {
    // Given
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf(library1)),
      UnusedDirectComponent(project2, mutableSetOf(library2)),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply {
      unusedDependenciesBehavior = Warn(setOf(project1.identifier))
    })

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNotNull(removeAdvice, "Remove advice should not be null")

    val expected = sortedSetOf(
      Advice.remove(project2),
      Advice.remove(project3)
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun removeAdviceRespectsIgnoreSomeFromAnyRule() {
    // Given
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf(library1)),
      UnusedDirectComponent(project2, mutableSetOf(library2)),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = emptyList(),
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute(FilterSpecBuilder().apply { anyBehavior = Warn(setOf(project1.identifier)) })

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNotNull(removeAdvice, "Remove advice should not be null")

    val expected = sortedSetOf(
      Advice.remove(project2),
      Advice.remove(project3)
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }

  @Test fun removeAdviceWillNotAdviseRemovingACompileOnlyDependency() {
    // Given
    val allComponents = listOf(
      Component(project1, isTransitive = false, isCompileOnlyAnnotations = true, classes = emptySet()),
      Component(project2, isTransitive = false, isCompileOnlyAnnotations = false, classes = emptySet()),
      Component(project3, isTransitive = false, isCompileOnlyAnnotations = false, classes = emptySet())
    )
    val unusedDirectComponents = listOf(
      UnusedDirectComponent(project1, mutableSetOf()),
      UnusedDirectComponent(project2, mutableSetOf()),
      UnusedDirectComponent(project3, mutableSetOf())
    )
    val advisor = Advisor(
      allComponents = allComponents,
      unusedDirectComponents = unusedDirectComponents,
      usedTransitiveComponents = emptyList(),
      abiDeps = emptyList(),
      allDeclaredDeps = emptyList(),
      unusedProcs = emptySet()
    )
    val computedAdvice = advisor.compute()

    // When
    var consoleReport = ConsoleReport.from(computedAdvice)
    val removeAdvice = AdvicePrinter(consoleReport).getRemoveAdvice()

    // Then
    assertNotNull(removeAdvice, "Remove advice should not be null")

    val expected = sortedSetOf(
      Advice.remove(project2),
      Advice.remove(project3)
    )
    val actual = computedAdvice.getAdvices().filterToOrderedSet { it.isRemove() }
    assertEquals(expected, actual, "Expected $expected\nActual   $actual\n")
  }
}
