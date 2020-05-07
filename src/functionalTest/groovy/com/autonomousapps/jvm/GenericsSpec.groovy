package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.advice.Advice
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.jvm.projects.GenericsProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GenericsSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "generics are accounted for (#gradleVersion)"() {
    given:
    jvmProject = new GenericsProject().jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, ':buildHealth')

    then: 'there is no advice'
    assertThat(jvmProject.adviceForFirstProject()).containsExactlyElementsIn([] as List<Advice>)

    where:
    gradleVersion << gradleVersions()
  }
}
