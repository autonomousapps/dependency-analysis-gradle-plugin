// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ArtifactsReportCacheProject

import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class ArtifactsReportCacheSpec extends AbstractJvmSpec {

  def "artifact reports are sensitive to resolved versions (#gradleVersion)"() {
    given:
    def project = new ArtifactsReportCacheProject()
    gradleProject = project.gradleProject
    def compileTask = ':proj:artifactsReportMain'
    def runtimeTask = ':proj:artifactsReportRuntimeMain'

    when: 'First build with version 1.0 selected by the BOM'
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth', '-DbomVersion=1.0')

    then: 'Both reports execute and contain version 1.0'
    assertAbout(buildTasks()).that(result.task(compileTask)).succeeded()
    assertAbout(buildTasks()).that(result.task(runtimeTask)).succeeded()
    assertReportsContainVersion(project, '1.0')
    assertThat(actualProjectAdvice('proj').dependencyAdvice).isEmpty()

    when: 'The build is cleaned and repeated with the same resolved version'
    result = build(gradleVersion, gradleProject.rootDir, 'clean', ':buildHealth', '-DbomVersion=1.0')

    then: 'The unchanged reports are restored from cache'
    assertAbout(buildTasks()).that(result.task(compileTask)).fromCache()
    assertAbout(buildTasks()).that(result.task(runtimeTask)).fromCache()
    assertReportsContainVersion(project, '1.0')
    assertThat(actualProjectAdvice('proj').dependencyAdvice).isEmpty()

    when: 'The BOM selects version 2.0 with a byte-identical artifact'
    result = build(gradleVersion, gradleProject.rootDir, 'clean', ':buildHealth', '-DbomVersion=2.0')

    then: 'Both reports execute with the new identity and advice remains correct'
    assertAbout(buildTasks()).that(result.task(compileTask)).succeeded()
    assertAbout(buildTasks()).that(result.task(runtimeTask)).succeeded()
    assertReportsContainVersion(project, '2.0')
    assertThat(project.compileArtifactsReport).doesNotContain('"resolvedVersion":"1.0"')
    assertThat(project.runtimeArtifactsReport).doesNotContain('"resolvedVersion":"1.0"')
    assertThat(actualProjectAdvice('proj').dependencyAdvice).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  private static void assertReportsContainVersion(ArtifactsReportCacheProject project, String version) {
    assertThat(project.compileArtifactsReport).contains("\"resolvedVersion\":\"$version\"")
    assertThat(project.runtimeArtifactsReport).contains("\"resolvedVersion\":\"$version\"")
  }
}
