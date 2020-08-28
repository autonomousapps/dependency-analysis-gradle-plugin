package com.autonomousapps.android

import com.autonomousapps.android.projects.NativeLibProject
import spock.lang.Ignore
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class NativeLibSpec extends AbstractAndroidSpec {

  @Ignore("Need to find a simple way to get the flat aar into the test project's libs dir")
  @Unroll
  def "leakcanary is not reported as unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new NativeLibProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.expectedAdvice).containsExactlyElementsIn(project.actualAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
