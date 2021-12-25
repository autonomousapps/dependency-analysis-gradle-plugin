package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GradlePluginProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GradlePluginSpec extends AbstractJvmSpec {

  // Let's avoid this kind of advice:
  // Transitively used dependencies that should be declared directly as indicated:
  // - api("Gradle TestKit:null")
  // - api("Gradle API:null")
  def "don't suggest declaring parts of the gradle distribution (#gradleVersion)"() {
    given:
    def project = new GradlePluginProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
