package com.autonomousapps.android

import com.autonomousapps.android.projects.SettingsProject
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings('GroovyAssignabilityCheck')
final class SettingsSpec extends AbstractAndroidSpec {

  def "BuildHealthPlugin can be applied to settings script in android-kotlin project (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new SettingsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrixSettingsApi()
  }
}
