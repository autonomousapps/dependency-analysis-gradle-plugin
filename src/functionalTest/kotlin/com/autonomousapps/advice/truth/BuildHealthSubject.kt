// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.advice.truth

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.ProjectAdvice
import com.google.common.truth.*
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

class BuildHealthSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: Set<ProjectAdvice>?
) : IterableSubject(failureMetadata, actual) {

  companion object {
    private val FACTORY: Factory<BuildHealthSubject, Set<ProjectAdvice>> = Factory { metadata, actual ->
      BuildHealthSubject(metadata, actual)
    }

    @JvmStatic
    fun buildHealth(): Factory<BuildHealthSubject, Set<ProjectAdvice>> = FACTORY

    @JvmStatic
    fun assertThat(actual: Set<ProjectAdvice>?): BuildHealthSubject {
      return assertAbout(buildHealth()).that(actual)
    }

    private val DEPENDENCY_EQUIVALENCE: Correspondence<ProjectAdvice, Pair<String, Set<Advice>>> =
      Correspondence.transforming(::dependencyAdvice, "has dependency advice")

    private val PLUGIN_EQUIVALENCE: Correspondence<ProjectAdvice, Pair<String, Set<PluginAdvice>>> =
      Correspondence.transforming(::pluginAdvice, "has plugin advice")

    private val MODULE_EQUIVALENCE: Correspondence<ProjectAdvice, Pair<String, Set<ModuleAdvice>>> =
      Correspondence.transforming(::moduleAdvice, "has module advice")

    private val SHOULD_FAIL_EQUIVALENCE: Correspondence<ProjectAdvice, Pair<String, Boolean>> =
      Correspondence.transforming(::shouldFail, "should fail")

    private fun dependencyAdvice(projectAdvice: ProjectAdvice?): Pair<String, Set<Advice>> {
      Truth.assertThat(projectAdvice).isNotNull()
      return projectAdvice!!.projectPath to projectAdvice.dependencyAdvice
    }

    private fun pluginAdvice(projectAdvice: ProjectAdvice?): Pair<String, Set<PluginAdvice>> {
      Truth.assertThat(projectAdvice).isNotNull()
      return projectAdvice!!.projectPath to projectAdvice.pluginAdvice
    }

    private fun moduleAdvice(projectAdvice: ProjectAdvice?): Pair<String, Set<ModuleAdvice>> {
      Truth.assertThat(projectAdvice).isNotNull()
      return projectAdvice!!.projectPath to projectAdvice.moduleAdvice
    }

    private fun shouldFail(projectAdvice: ProjectAdvice?): Pair<String, Boolean> {
      Truth.assertThat(projectAdvice).isNotNull()
      return projectAdvice!!.projectPath to projectAdvice.shouldFail
    }
  }

  fun containsExactlyDependencyAdviceIn(expected: Iterable<ProjectAdvice>): Ordered {
    if (actual == null) failWithActual(simpleFact("build health was null"))
    assertThat(actual)
      .comparingElementsUsing(SHOULD_FAIL_EQUIVALENCE)
      .containsExactlyElementsIn(expected.map { it.projectPath to it.shouldFail })
    return Truth.assertThat(actual!!.associate { it.projectPath to it.dependencyAdvice })
      .containsExactlyEntriesIn(expected.associate { it.projectPath to it.dependencyAdvice })
  }

  fun containsExactlyDependencyAdviceIn(expected: Map<String, Set<Advice>>): Ordered {
    if (actual == null) failWithActual(simpleFact("build health was null"))
    return assertThat(actual)
      .comparingElementsUsing(DEPENDENCY_EQUIVALENCE)
      .containsExactlyElementsIn(pairs(expected))
  }

  fun isEquivalentIgnoringModuleAdvice(expected: Iterable<ProjectAdvice>) {
    if (actual == null) failWithActual(simpleFact("build health was null"))
    assertThat(actual)
      .comparingElementsUsing(DEPENDENCY_EQUIVALENCE)
      .containsExactlyElementsIn(pairs(expected) { it.dependencyAdvice })
    assertThat(actual)
      .comparingElementsUsing(PLUGIN_EQUIVALENCE)
      .containsExactlyElementsIn(pairs(expected) { it.pluginAdvice })
    assertThat(actual)
      .comparingElementsUsing(SHOULD_FAIL_EQUIVALENCE)
      .containsExactlyElementsIn(expected.map { it.projectPath to it.shouldFail })
  }

  fun containsExactlyModuleAdviceIn(expected: Iterable<ProjectAdvice>): Ordered {
    if (actual == null) failWithActual(simpleFact("build health was null"))
    assertThat(actual)
      .comparingElementsUsing(SHOULD_FAIL_EQUIVALENCE)
      .containsExactlyElementsIn(expected.map { it.projectPath to it.shouldFail })
    return assertThat(actual)
      .comparingElementsUsing(MODULE_EQUIVALENCE)
      .containsExactlyElementsIn(pairs(expected) { it.moduleAdvice })
  }

  fun containsExactlyModuleAdviceIn(expected: Map<String, Set<ModuleAdvice>>): Ordered {
    if (actual == null) failWithActual(simpleFact("build health was null"))
    return assertThat(actual)
      .comparingElementsUsing(MODULE_EQUIVALENCE)
      .containsExactlyElementsIn(pairs(expected))
  }

  private fun <T> pairs(map: Map<String, Set<T>>): Iterable<Pair<String, Set<T>>> = map.map { it.key to it.value }

  private fun <R> pairs(
    projectAdvice: Iterable<ProjectAdvice>,
    transform: (ProjectAdvice) -> Set<R>
  ): Iterable<Pair<String, Set<R>>> = projectAdvice.map { it.projectPath to transform(it) }
}
