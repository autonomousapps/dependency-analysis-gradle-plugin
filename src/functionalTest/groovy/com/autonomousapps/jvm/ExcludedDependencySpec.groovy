package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ExcludedDependencyProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ExcludedDependencySpec extends AbstractJvmSpec {

  def "suggests removing declared excluded dependencies (#gradleVersion)"() {
    given:
    def project = new ExcludedDependencyProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
