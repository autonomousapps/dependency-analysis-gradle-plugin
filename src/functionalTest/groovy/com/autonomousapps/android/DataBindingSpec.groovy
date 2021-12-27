package com.autonomousapps.android

import com.autonomousapps.fixtures.DataBindingProject
import com.autonomousapps.internal.android.AgpVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class DataBindingSpec extends AbstractAndroidSpec {

  def "dataBinding dependencies are not reported (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    assertThat(androidProject.adviceFor(project.appSpec)).containsExactlyElementsIn(project.expectedAdviceForApp)

    where:
    // AGP versions before 4.x will throw java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException
    // when compiled and run with Java 11. So just don't bother running those tests.
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.version('4.0.0'))
  }
}
