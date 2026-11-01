// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TransitiveExposureProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class TransitiveExposureSpec extends AbstractJvmSpec {

  private static final String FLAG = '-Ddependency.analysis.transitive.exposure=true'

  def "removal advice for a transitively exposed dependency is reported by default (#gradleVersion)"() {
    given:
    def project = new TransitiveExposureProject()
    gradleProject = project.gradleProject

    when: 'the filter is not enabled'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the false positive is present, as it is today'
    assertThat(removalAdviceFor(project.actualProjectAdvice(), ':middle')).contains(':producer')

    where:
    gradleVersion << gradleVersions()
  }

  def "enabling the filter suppresses the transitively exposed removal advice (#gradleVersion)"() {
    given:
    def project = new TransitiveExposureProject()
    gradleProject = project.gradleProject

    when: 'the filter is enabled'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', FLAG)

    then: "':consumer' reaches ':producer' through ':middle', so the advice is withheld"
    assertThat(removalAdviceFor(project.actualProjectAdvice(), ':middle')).doesNotContain(':producer')

    where:
    gradleVersion << gradleVersions()
  }

  def "the filter only withholds removal advice, leaving other advice intact (#gradleVersion)"() {
    given:
    def unfiltered = new TransitiveExposureProject()
    def filtered = new TransitiveExposureProject()
    gradleProject = filtered.gradleProject

    when:
    build(gradleVersion, unfiltered.gradleProject.rootDir, 'buildHealth')

    and:
    build(gradleVersion, filtered.gradleProject.rootDir, 'buildHealth', FLAG)

    then: 'the only difference is the suppressed removal advice'
    def before = allAdvice(unfiltered.actualProjectAdvice())
    def after = allAdvice(filtered.actualProjectAdvice())

    assertThat(after).isNotEmpty()
    assertThat(before).containsAtLeastElementsIn(after)
    assertThat(before - after).allMatch { Advice it -> it.isAnyRemove() }

    where:
    gradleVersion << gradleVersions()
  }

  private static Set<Advice> allAdvice(Set<ProjectAdvice> projectAdvice) {
    return projectAdvice.collectMany { it.dependencyAdvice }.toSet()
  }

  /** The identifiers this project is being told to remove. */
  private static Set<String> removalAdviceFor(Set<ProjectAdvice> projectAdvice, String projectPath) {
    return projectAdvice
      .find { it.projectPath == projectPath }
      ?.dependencyAdvice
      ?.findAll { it.isAnyRemove() }
      ?.collect { it.coordinates.identifier }
      ?.toSet() ?: [] as Set<String>
  }
}
