package com.autonomousapps

import com.autonomousapps.fixtures.FacadeProject
import com.autonomousapps.fixtures.jvm.JvmProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FacadeFilterSpec extends AbstractFunctionalSpec {

  private JvmProject jvmProject = null

  def cleanup() {
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
    }
  }

  @Unroll
  def "kotlin stdlib is a facade dependency by default (#gradleVersion)"() {
    given:
    def jvmProject = new FacadeProject().jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, 'buildHealth')

    then: 'no advice'
    def actualAdvice = jvmProject.adviceForFirstProject()
    assertThat(actualAdvice).containsExactlyElementsIn(FacadeProject.expectedFacadeAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "kotlin stdlib should be changed when NOT a facade (#gradleVersion)"() {
    given:
    def additions = """\
      dependencyAnalysis {
        setFacadeGroups()
      }
    """.stripIndent()
    def jvmProject = new FacadeProject(additions).jvmProject

    when:
    build(gradleVersion, jvmProject.rootDir, 'buildHealth')

    then: 'advice to change stdlib deps'
    def actualAdvice = jvmProject.adviceForFirstProject()
    assertThat(actualAdvice).containsExactlyElementsIn(FacadeProject.expectedNoFacadeAdvice())

    where:
    gradleVersion << gradleVersions()
  }
}
