package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AnnotationsImplementationProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AnnotationsImplementationSpec extends AbstractJvmSpec {

  def "classes used in runtime-retained annotations are implementation (#gradleVersion)"() {
    given:
    def project = new AnnotationsImplementationProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
