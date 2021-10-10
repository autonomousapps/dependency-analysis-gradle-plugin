package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiUsedClassProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiUsedClassSpec extends AbstractJvmSpec {

  def "do not suggest that ABI dependencies are unused (#gradleVersion)"() {
    given:
    def project = new AbiUsedClassProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdvice('lib')).containsExactlyElementsIn([])

    where:
    gradleVersion << gradleVersions()
  }

}
