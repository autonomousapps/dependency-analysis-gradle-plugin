package com.autonomousapps.android

import com.autonomousapps.fixtures.ViewBindingProject
import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ViewBindingSpec extends AbstractAndroidSpec {

  @Unroll
  def "viewBinding dependencies are not reported (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ViewBindingProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdviceForApp = androidProject.adviceFor(project.appSpec)
    def expectedAdviceForApp = project.expectedAdviceForApp
    assertThat(actualAdviceForApp).containsExactlyElementsIn(expectedAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.version('3.6'))
  }
}
