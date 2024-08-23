package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SubprojectProject

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class SubprojectSpec extends AbstractJvmSpec {

  def "plugin doesn't have to be applied to root project (#gradleVersion)"() {
    given:
    def project = new SubprojectProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'lib:projectHealth')

    then:
    assertThat(project.actualProjectAdvice()).isEqualTo(project.expectedProjectAdvice)

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    result.output.contains("Task 'buildHealth' not found")

    where:
    gradleVersion << gradleVersions()
  }
}
