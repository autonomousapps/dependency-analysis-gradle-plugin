package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiExclusionsProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiExclusionsSpec extends AbstractJvmSpec {

  @Unroll
  def "abi exclusion smoke test (#gradleVersion)"() {
    given:
    def project = new AbiExclusionsProject()
    jvmProject = project.jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, ':buildHealth')

    then:
    assertThat(jvmProject.adviceForFirstProject()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
