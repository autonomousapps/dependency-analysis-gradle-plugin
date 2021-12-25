package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GenericsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GenericsSpec extends AbstractJvmSpec {

  def "generics are accounted for (#gradleVersion)"() {
    given:
    def project = new GenericsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
