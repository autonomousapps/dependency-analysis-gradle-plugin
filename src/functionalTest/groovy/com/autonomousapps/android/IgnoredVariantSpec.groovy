package com.autonomousapps.android

import com.autonomousapps.android.projects.AllVariantsIgnoredProject
import com.autonomousapps.android.projects.ReleaseVariantIgnoredProject
import com.autonomousapps.android.projects.DebugVariantIgnoredProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class IgnoredVariantSpec extends AbstractAndroidSpec {

  def "plugin ignore android variants (#gradleVersion AGP #agpVersion ignored debug)"() {
    given:
    def project = new DebugVariantIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "plugin ignore android variants (#gradleVersion AGP #agpVersion ignored release)"() {
    given:
    def project = new ReleaseVariantIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "plugin ignore android variants (#gradleVersion AGP #agpVersion ignored all variants)"() {
    given:
    def project = new AllVariantsIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
