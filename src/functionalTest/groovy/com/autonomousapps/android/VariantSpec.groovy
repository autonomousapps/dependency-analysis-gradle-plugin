package com.autonomousapps.android

import com.autonomousapps.android.projects.VariantProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class VariantSpec extends AbstractAndroidSpec {

  @Unroll
  def "plugin understands android variants (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new VariantProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
