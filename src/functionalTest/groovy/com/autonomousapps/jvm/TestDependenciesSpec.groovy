package com.autonomousapps.jvm

import com.autonomousapps.FlagsKt
import com.autonomousapps.jvm.projects.TestBundleProject
import com.autonomousapps.jvm.projects.TestDependenciesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestDependenciesSpec extends AbstractJvmSpec {

  def "unused test dependencies are reported (#gradleVersion)"() {
    given:
    def project = new TestDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "test dependencies should not be reported when test analysis is disabled (#gradleVersion)"() {
    given:
    def project = new TestDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', "-D${FlagsKt.FLAG_TEST_ANALYSIS}=false")

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdviceWithoutTest)

    where:
    gradleVersion << gradleVersions()
  }

  def "bundles work for test dependencies (#gradleVersion)"() {
    given:
    def project = new TestBundleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
