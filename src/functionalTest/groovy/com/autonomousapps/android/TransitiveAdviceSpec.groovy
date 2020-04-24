package com.autonomousapps.android

import com.autonomousapps.fixtures.TransitiveAdviceAndroidProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class TransitiveAdviceSpec extends AbstractAndroidSpec {

  def "remove and add advice contains information about transitives and parents (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TransitiveAdviceAndroidProject(agpVersion)
    def androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor('app')
    assertThat(project.expectedAdviceForApp).containsExactlyElementsIn(actualAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
