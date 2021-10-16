package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GradleApiProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GradleApiSpec extends AbstractJvmSpec {

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
