package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SecurityProviderProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SecurityProviderSpec extends AbstractJvmSpec {

  def "does not recommend removing conscrypt (#gradleVersion)"() {
    given:
    def project = new SecurityProviderProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
