package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GradleApiProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class GradleApiSpec extends AbstractJvmSpec {
  @Unroll
  def "gradleApi doesn't break the build (#gradleVersion)"() {
    given:
    def project = new GradleApiProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'and there is no advice'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
