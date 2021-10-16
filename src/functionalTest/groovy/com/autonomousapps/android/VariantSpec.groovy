package com.autonomousapps.android

import com.autonomousapps.android.projects.VariantProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class VariantSpec extends AbstractAndroidSpec {

  def "plugin understands android variants (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new VariantProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'clean', 'buildHealth', '--no-build-cache')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
