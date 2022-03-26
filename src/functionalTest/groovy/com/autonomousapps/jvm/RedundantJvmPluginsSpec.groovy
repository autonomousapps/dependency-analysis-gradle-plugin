package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RedundantJvmPluginsProject

import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class RedundantJvmPluginsSpec extends AbstractJvmSpec {

  def "kotlin-jvm plugin is redundant (#gradleVersion)"() {
    given:
    def project = new RedundantJvmPluginsProject()
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:

    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
