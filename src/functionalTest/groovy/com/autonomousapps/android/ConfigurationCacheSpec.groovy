package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidAssetsProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class ConfigurationCacheSpec extends AbstractAndroidSpec {

  def "buildHealth succeeds when configuration-cache flag is used (#gradleVersion AGP #agpVersion)"() {
    given: 'A complicated Android project'
    def project = new AndroidAssetsProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '--configuration-cache'
    )

    then: 'buildHealth produces expected results'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    and: 'generateBuildHealth succeeded'
    assertAbout(buildTasks()).that(result.task(':generateBuildHealth')).succeeded()

    and: 'This plugin is not yet compatible with the configuration cache'
    assertThat(result.output).contains('0 problems were found storing the configuration cache.')
    assertThat(result.output).contains('Configuration cache entry discarded.')

    when: 'We build again'
    result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '--configuration-cache'
    )

    then: 'buildHealth produces expected results'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    and: 'generateBuildHealth was up-to-date'
    assertAbout(buildTasks()).that(result.task(':generateBuildHealth')).upToDate()

    and: 'This plugin is not yet compatible with the configuration cache'
    assertThat(result.output).contains('0 problems were found storing the configuration cache.')
    assertThat(result.output).contains('Configuration cache entry discarded.')

    where: 'Min support for this is Gradle 7.5'
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
