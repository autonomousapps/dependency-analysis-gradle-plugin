package com.autonomousapps.android

import com.autonomousapps.fixtures.ServiceLoaderProject
import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Ignore
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ServiceLoaderSpec extends AbstractAndroidSpec {

  @Ignore("This test will fail until the work on service loaders is complete")
  @Unroll
  def "service-loading libraries are not reported as unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ServiceLoaderProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdviceForApp = androidProject.adviceFor(project.appSpec)
    def expectedAdviceForApp = project.expectedAdviceForApp
    assertThat(expectedAdviceForApp).containsExactlyElementsIn(actualAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.version('3.6'))
  }
}
