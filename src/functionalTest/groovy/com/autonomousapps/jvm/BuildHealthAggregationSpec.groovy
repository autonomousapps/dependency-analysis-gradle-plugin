// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.jvm.projects.ManyProjectsProject
import com.autonomousapps.model.BuildHealth

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class BuildHealthAggregationSpec extends AbstractJvmSpec {

  def "aggregation covers every project in a wide build (#gradleVersion)"() {
    given:
    def project = new ManyProjectsProject(25)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'no project is dropped while the report is streamed out'
    def advised = project.actualProjectAdvice()
      .findAll { !it.dependencyAdvice.isEmpty() }
      .collect { it.projectPath }
      .toSet()

    assertThat(advised).containsExactlyElementsIn(project.expectedProjectPathsWithAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "the aggregate report's counts match its contents (#gradleVersion)"() {
    given:
    def project = new ManyProjectsProject(25)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the summary written after the advice array agrees with the advice array'
    def report = gradleProject.singleArtifact(':', OutputPathsKt.getFinalAdvicePathV2())
    BuildHealth buildHealth = MoshiUtils.MOSHI.adapter(BuildHealth).fromJson(report.asPath.text)

    assertThat(buildHealth.projectCount).isEqualTo(buildHealth.projectAdvice.size())
    assertThat(buildHealth.unusedCount).isEqualTo(
      buildHealth.projectAdvice.collectMany { it.dependencyAdvice }.count { it.isRemove() }
    )

    where:
    gradleVersion << gradleVersions()
  }
}
