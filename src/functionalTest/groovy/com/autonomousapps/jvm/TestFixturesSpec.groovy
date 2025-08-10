// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TestFixturesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestFixturesSpec extends AbstractJvmSpec {

  def "detects test fixtures usage (#gradleVersion)"() {
    given:
    def project = new TestFixturesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    when: 'we fix dependencies'
    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')

    then:
    def buildScript = gradleProject.rootDir.toPath().resolve('consumer/build.gradle.kts').text
    buildScript.contains(
      '''\
      dependencies {
        api(project(":producer"))
        implementation(testFixtures(project(":producer")))
      }'''.stripIndent()
    )

    when: 'we reason about the test-fixtures dependency'
    def result = build(
      gradleVersion, gradleProject.rootDir,
      ':consumer:reason',
      '--id', ':producer',
      '--capability', 'test-fixtures'
    )

    then:
    result.output.contains('* Uses 1 class: com.example.producer.FakeProducer (implies implementation).')

    where:
    gradleVersion << gradleVersions()
  }
}
