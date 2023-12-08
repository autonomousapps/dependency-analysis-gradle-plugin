package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TypealiasProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TypealiasSpec extends AbstractJvmSpec {

  def "typealiases are not ignored (#gradleVersion)"() {
    given:
    def project = new TypealiasProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
