package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GradleBuildSrcConventionMultiConfigProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GradleBuildSrcConventionMultiConfigSpec extends AbstractJvmSpec {

  def "allow common configuration through conventions and project level configuration (#gradleVersion)"() {
    given:
    def project = new GradleBuildSrcConventionMultiConfigProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
