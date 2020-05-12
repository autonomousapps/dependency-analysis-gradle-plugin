package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.jvm.projects.ApplicationProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ApplicationSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "can analyze jvm application projects (#gradleVersion)"() {
    given:
    jvmProject = new ApplicationProject().jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, ':buildHealth')

    then:
    assertThat(jvmProject.adviceForFirstProject()).containsExactlyElementsIn(ApplicationProject.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
