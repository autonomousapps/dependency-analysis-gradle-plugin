package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RegexExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class RegexExclusionsSpec extends AbstractJvmSpec {
  def "project can exclude dependencies by regex patterns (#gradleVersion)"() {
    given:
    def project = new RegexExclusionsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, "buildHealth")

    then:
    assertThat(project.actualProjectAdvice().isEmpty())

    where:
    gradleVersion << gradleVersions()
  }
}
