package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RenamingProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RenamingSpec extends AbstractJvmSpec {

  @Unroll
  def "dependencies are renamed when renamer is used (#gradleVersion)"() {
    given:
    def project = new RenamingProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdviceConsole()).contains(project.expectedRenamedConsoleReport())

    where:
    gradleVersion << gradleVersions()
  }
}
