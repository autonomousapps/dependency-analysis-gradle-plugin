// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.jvm.projects.GraphViewProjectEdgeCacheProject

import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class GraphViewProjectEdgeCacheSpec extends AbstractJvmSpec {

  def "graphViewTask is sensitive to new transitive project edges (#gradleVersion)"() {
    given:
    def project = new GraphViewProjectEdgeCacheProject()
    gradleProject = project.gradleProject
    def task = ':consumer:graphViewMain'

    when: 'First build, without the direct -> transitive edge'
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')
    def graphCompilePath = OutputPathsKt.getGraphCompilePath('main')
    def graphOutput = gradleProject.singleArtifact('consumer', graphCompilePath).asFile

    then: 'Task executed and transitive is not in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).doesNotContain(':transitive')

    when: 'Second build, after the direct project adds an api dependency on transitive'
    result = build(gradleVersion, gradleProject.rootDir, 'clean', ':buildHealth', '-Dedge=true')

    then: 'Task executed (not FROM_CACHE) and transitive is in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).contains(':transitive')

    where:
    gradleVersion << gradleVersions()
  }

  def "stale project graph does not suppress dependency advice (#gradleVersion)"() {
    given:
    def project = new GraphViewProjectEdgeCacheProject()
    gradleProject = project.gradleProject

    when: 'First build, without the direct -> transitive edge'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'There is no advice'
    assertThat(actualProjectAdvice('consumer').dependencyAdvice).isEmpty()

    when: 'Second build, after the direct project adds an api dependency on transitive'
    build(gradleVersion, gradleProject.rootDir, 'clean', ':buildHealth', '-Dedge=true')

    then: 'Advises declaring the used transitive dependency directly'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
