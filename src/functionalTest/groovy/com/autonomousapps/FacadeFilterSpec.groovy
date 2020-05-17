package com.autonomousapps

import com.autonomousapps.android.projects.FacadeProject
import com.autonomousapps.jvm.AbstractJvmSpec
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FacadeFilterSpec extends AbstractJvmSpec {

  @Unroll
  def "kotlin stdlib is a facade dependency by default (#gradleVersion)"() {
    given:
    def project = new FacadeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'no advice'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedFacadeAdvice())

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
    def project = new FacadeProject(additions)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'advice to change stdlib deps'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedNoFacadeAdvice())

    where:
    gradleVersion << gradleVersions()
  }
}
