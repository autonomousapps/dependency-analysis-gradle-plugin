package com.autonomousapps.android

import com.autonomousapps.android.projects.DataBindingUsagesExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class DataBindingUsagesExclusionsSpec extends AbstractAndroidSpec {

  def "doesn't report unused dataBinding module by default (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingUsagesExclusionsProject(agpVersion, false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }

  def "reports unused dataBinding module when DataBinderMapperImpl usages are excluded (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingUsagesExclusionsProject(agpVersion, true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }
}
