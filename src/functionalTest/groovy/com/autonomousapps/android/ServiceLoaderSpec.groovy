package com.autonomousapps.android

import com.autonomousapps.android.projects.ServiceLoaderProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class ServiceLoaderSpec extends AbstractAndroidSpec {

  def "service-loading libraries are not reported as unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ServiceLoaderProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
