package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.MinimalAdviceProject
import com.autonomousapps.jvm.projects.MinimalFailProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings('DuplicatedCode')
final class MinimalAdviceSpec extends AbstractJvmSpec {

  def "minimized advice skips impl dependencies (#gradleVersion)"() {
    given:
    def project = new MinimalAdviceProject.Changes(false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'Minimized advice contains expected elements'
    def minimized = actualMinimizedBuildHealth()
    assertThat(minimized).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "minimized advice doesn't fail on strict advice (#gradleVersion)"() {
    given:
    def project = new MinimalFailProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'Minimized advice contains expected elements'
    def minimized = actualMinimizedBuildHealth()
    assertThat(minimized).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "minimized advice does not skip api dependencies (#gradleVersion)"() {
    given:
    def project = new MinimalAdviceProject.SomeChanges(false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'Minimized advice strips unnecessary strict advice'
    def minimized = actualMinimizedBuildHealth()
    assertThat(minimized).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "minimized advice matches strict advice (#gradleVersion)"() {
    given:
    def project = new MinimalAdviceProject.NoChanges(false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'Minimized and strict advice match'
    def minimized = actualMinimizedBuildHealth()
    assertThat(minimized).containsExactlyElementsIn(actualStrictBuildHealth())

    and: 'Minimized advice strips unnecessary strict advice'
    assertThat(minimized).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
