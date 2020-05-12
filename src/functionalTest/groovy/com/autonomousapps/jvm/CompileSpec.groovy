package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.jvm.projects.CompileProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CompileSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "compile conf is correctly accounted for (#gradleVersion)"() {
    given:
    jvmProject = new CompileProject().jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, ':buildHealth')

    then:
    assertThat(jvmProject.adviceForFirstProject()).containsExactlyElementsIn(CompileProject.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
