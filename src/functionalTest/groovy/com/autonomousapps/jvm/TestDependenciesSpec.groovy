package com.autonomousapps.jvm

import com.autonomousapps.FlagsKt
import com.autonomousapps.jvm.projects.TestBundleProject
import com.autonomousapps.jvm.projects.TestDependenciesProject
import com.autonomousapps.jvm.projects.TestDependenciesProject2
import org.gradle.util.GradleVersion
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealthWithoutTest)

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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  @IgnoreIf({ PreconditionContext it -> it.sys.'dependency.analysis.old.model' == 'true' })
  def "don't advise removing test declarations when test analysis is disabled (#gradleVersion analyzeTests=#analyzeTests)"() {
    given:
    def project = new TestDependenciesProject2()
    gradleProject = project.gradleProject

    when:
    def flag = "-D${FlagsKt.FLAG_TEST_ANALYSIS}=$analyzeTests"
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth', flag)

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, analyzeTests] << multivariableDataPipe(gradleVersions(), [true, false])
  }
}
