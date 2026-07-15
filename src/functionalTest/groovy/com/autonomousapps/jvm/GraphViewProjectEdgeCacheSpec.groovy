// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GraphViewProjectEdgeCacheProject
import com.autonomousapps.model.Advice
import org.gradle.util.GradleVersion

import static com.autonomousapps.AdviceHelper.projectCoordinates
import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class GraphViewProjectEdgeCacheSpec extends AbstractJvmSpec {

  def "graphViewTask is sensitive to new transitive project edges"() {
    given:
    def project = new GraphViewProjectEdgeCacheProject()
    gradleProject = project.gradleProject
    def task = ':consumer:graphViewMain'
    def gradleVersion = GradleVersion.current()
    def graphOutput = new File(
      gradleProject.rootDir,
      'consumer/build/reports/dependency-analysis/main/graph/graph-compile.json'
    )

    when: 'First build, without the direct -> transitive edge'
    def result = build(gradleVersion, gradleProject.rootDir, task, '--build-cache')

    then: 'Task executed and transitive is not in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).doesNotContain(':transitive')

    when: 'Second build, after the direct project adds an api dependency on transitive'
    result = build(gradleVersion, gradleProject.rootDir, 'clean', task, '--build-cache', '-Dedge=true')

    then: 'Task executed (not FROM_CACHE) and transitive is in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).contains(':transitive')
  }

  def "stale project graph does not suppress dependency advice"() {
    given:
    def project = new GraphViewProjectEdgeCacheProject()
    gradleProject = project.gradleProject
    def task = ':consumer:projectHealth'
    def gradleVersion = GradleVersion.current()

    when: 'First build, without the direct -> transitive edge'
    build(gradleVersion, gradleProject.rootDir, task, '--build-cache')

    then: 'There is no advice'
    assertThat(actualProjectAdvice('consumer').dependencyAdvice).isEmpty()

    when: 'Second build, after the direct project adds an api dependency on transitive'
    build(gradleVersion, gradleProject.rootDir, 'clean', task, '--build-cache', '-Dedge=true')

    then: 'DAGP advises declaring the used transitive dependency directly'
    assertThat(actualProjectAdvice('consumer').dependencyAdvice)
      .containsExactly(Advice.ofAdd(projectCoordinates(':transitive'), 'implementation'))
  }
}
