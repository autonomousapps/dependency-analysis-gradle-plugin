package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RedundantPluginsProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class RedundantPluginsSpec extends AbstractJvmSpec {

  @Unroll
  def "kotlin-jvm plugin is redundant (#gradleVersion)"() {
    given:
    def project = new RedundantPluginsProject()
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:

    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
