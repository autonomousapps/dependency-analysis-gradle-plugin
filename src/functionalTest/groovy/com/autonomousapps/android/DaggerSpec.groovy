package com.autonomousapps.android

import com.autonomousapps.android.projects.DaggerProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DaggerSpec extends AbstractAndroidSpec {

  // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/479
  @SuppressWarnings('GroovyAssignabilityCheck')
  def "can introspect dagger annotation processor (#gradleVersion AGP #agpVersion)"() {
    given:
    def projectName = 'lib'
    def project = new DaggerProject(agpVersion, projectName)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualComprehensiveAdvice(projectName)).isEqualTo(project.expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
