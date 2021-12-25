package com.autonomousapps.jvm

import com.autonomousapps.FlagsKt
import com.autonomousapps.jvm.projects.TestBundleProject
import com.autonomousapps.jvm.projects.TestDependenciesProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.PendingFeatureIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: test support has not yet been added to v2
final class TestDependenciesSpec extends AbstractJvmSpec {

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
  def "unused test dependencies are reported (#gradleVersion)"() {
    given:
    def project = new TestDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  // This works in v2 ony by accident, by virtue of the fact that test support doesn't exist yet
  def "test dependencies should not be reported when test analysis is disabled (#gradleVersion)"() {
    given:
    def project = new TestDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', "-D${FlagsKt.FLAG_TEST_ANALYSIS}=false")

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealthWithoutTest)

    where:
    gradleVersion << gradleVersions()
  }

  // This works in v2 ony by accident, by virtue of the fact that test support doesn't exist yet
  def "bundles work for test dependencies (#gradleVersion)"() {
    given:
    def project = new TestBundleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
