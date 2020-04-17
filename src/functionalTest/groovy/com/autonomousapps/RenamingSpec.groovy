package com.autonomousapps

import com.autonomousapps.fixtures.RenamingProject
import com.autonomousapps.fixtures.jvm.JvmProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RenamingSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "dependencies are renamed when renamer is used (#gradleVersion)"() {
    given:
    def jvmProject = new RenamingProject().jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, 'buildHealth')

    then: 'renamed advice'
    def actualAdvice = jvmProject.adviceConsoleForFirstProject()
    assertThat(actualAdvice).contains(RenamingProject.expectedRenamedConsoleReport())

    where:
    gradleVersion << gradleVersions()
  }
}
