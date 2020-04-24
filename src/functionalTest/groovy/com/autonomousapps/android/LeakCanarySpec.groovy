package com.autonomousapps.android

import com.autonomousapps.fixtures.LeakCanaryProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class LeakCanarySpec extends AbstractAndroidSpec {

  @Unroll
  def "leakcanary is not reported as unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new LeakCanaryProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(expectedAdvice).containsExactlyElementsIn(actualAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
