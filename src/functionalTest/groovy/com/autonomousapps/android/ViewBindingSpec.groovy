package com.autonomousapps.android

import com.autonomousapps.fixtures.ViewBindingProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ViewBindingSpec extends AbstractAndroidSpec {

  def "viewBinding dependencies are not reported (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ViewBindingProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    assertThat(androidProject.adviceFor(project.appSpec)).containsExactlyElementsIn(project.expectedAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
