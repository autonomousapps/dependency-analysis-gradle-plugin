// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GraphViewProjectEdgeCacheProject
import org.gradle.util.GradleVersion

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

    when: 'First build, without the middle -> leaf edge'
    def result = build(gradleVersion, gradleProject.rootDir, task, '--build-cache')

    then: 'Task executed and leaf is not in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).doesNotContain(':leaf')

    when: 'Second build, after an upstream project adds an api dependency on leaf'
    result = build(gradleVersion, gradleProject.rootDir, 'clean', task, '--build-cache', '-Dedge=true')

    then: 'Task executed (not FROM_CACHE) and leaf is in the graph'
    assertAbout(buildTasks()).that(result.task(task)).succeeded()
    assertThat(graphOutput.text).contains(':leaf')
  }
}
