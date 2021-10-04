package com.autonomousapps.android

import com.autonomousapps.android.projects.ArbitraryFileProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ArbitraryFileSpec extends AbstractAndroidSpec {

  @Unroll
  def "doesn't blow up at random files in arbitrary locations (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ArbitraryFileProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}