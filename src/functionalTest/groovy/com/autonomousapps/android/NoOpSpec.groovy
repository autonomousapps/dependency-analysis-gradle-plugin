package com.autonomousapps.android

import com.autonomousapps.android.projects.NoOpProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class NoOpSpec extends AbstractAndroidSpec {

  def "can filter unused dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new NoOpProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
