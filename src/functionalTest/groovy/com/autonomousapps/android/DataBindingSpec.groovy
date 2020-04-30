package com.autonomousapps.android

import com.autonomousapps.fixtures.DataBindingProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class DataBindingSpec extends AbstractAndroidSpec {

  @Unroll
  def "dataBinding dependencies are not reported (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdviceForApp = androidProject.adviceFor(project.appSpec)
    def expectedAdviceForApp = project.expectedAdviceForApp
    assertThat(expectedAdviceForApp).containsExactlyElementsIn(actualAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
