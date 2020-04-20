package com.autonomousapps.android

import com.autonomousapps.fixtures.DataBindingProject
import com.autonomousapps.fixtures.ViewBindingProject
import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

final class BindingSpec extends AbstractAndroidSpec {

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
    expectedAdviceForApp == actualAdviceForApp

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.version('3.6'))
  }

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
    expectedAdviceForApp == actualAdviceForApp

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
