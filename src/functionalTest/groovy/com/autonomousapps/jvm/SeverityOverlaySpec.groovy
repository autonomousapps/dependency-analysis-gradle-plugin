package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SeverityOverlayProject

import static com.autonomousapps.jvm.projects.SeverityOverlayProject.Severity
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class SeverityOverlaySpec extends AbstractJvmSpec {

  def "project can upgrade severity (#gradleVersion)"() {
    given:
    def project = new SeverityOverlayProject(Severity.UPGRADE)
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'No need to test against all supported Gradle versions'
    gradleVersion << [gradleVersions().last()]
  }

  // Simply configuring a project-specific issue handler (even if empty) would downgrade severity.
  def "project configuration do not downgrade severity (#gradleVersion)"() {
    given:
    def project = new SeverityOverlayProject(Severity.UNCHANGED)
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'No need to test against all supported Gradle versions'
    gradleVersion << [gradleVersions().last()]
  }

  def "project can downgrade severity (#gradleVersion)"() {
    given:
    def project = new SeverityOverlayProject(Severity.DOWNGRADE)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'No need to test against all supported Gradle versions'
    gradleVersion << [gradleVersions().last()]
  }
}
