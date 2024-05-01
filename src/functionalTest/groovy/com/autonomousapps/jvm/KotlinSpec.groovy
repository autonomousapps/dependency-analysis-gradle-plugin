package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KotlinPrivateValProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class KotlinSpec extends AbstractJvmSpec {

  def "private vals are not part of the ABI (#gradleVersion)"() {
    given:
    def project = new KotlinPrivateValProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', ':producer')
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
