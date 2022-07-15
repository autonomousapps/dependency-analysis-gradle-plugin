package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.IncludedBuildProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/565
final class IncludedBuildSpec extends AbstractJvmSpec {

  def "doesn't crash in presence of an included build (#gradleVersion)"() {
    given:
    def project = new IncludedBuildProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'This new feature only works for Gradle 7.3+'
    gradleVersion << gradleVersions()
  }
}
