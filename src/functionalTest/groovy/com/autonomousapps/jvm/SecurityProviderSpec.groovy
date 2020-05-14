package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.jvm.projects.ApplicationProject
import com.autonomousapps.jvm.projects.SecurityProviderProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SecurityProviderSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "does not recommend removing conscrypt (#gradleVersion)"() {
    given:
    def project = new SecurityProviderProject()
    jvmProject = project.jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, ':buildHealth')

    then:
    assertThat(jvmProject.adviceForFirstProject()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
