package com.autonomousapps.android

import com.autonomousapps.android.projects.AllVariantsProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class VariantSpec extends AbstractAndroidSpec {

  def "plugin understands android variants (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AllVariantsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'clean', 'buildHealth', '--no-build-cache')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
