package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.Kotlin2Migration

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class Kotlin2MigrationSpec extends AbstractJvmSpec {

  def "buildHealth passes without testFixtures dependencies with Kotlin 1.9 (#gradleVersion)"() {
    given:
    def project = new Kotlin2Migration.CompilesWithoutTestFixturesDependencies()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "buildHealth fails without testFixtures dependencies with Kotlin 1.9 (#gradleVersion)"() {
    given:
    def project = new Kotlin2Migration.BuildHealthFailsWithoutTestFixturesDependencies()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "buildHealth passes with testFixtures dependency with Kotlin 2.0 (#gradleVersion)"() {
    given:
    def project = new Kotlin2Migration.CompilesWithTestFixturesDependency()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "compilation fails without testFixtures dependency with Kotlin 2.0 (#gradleVersion)"() {
    given:
    def project = new Kotlin2Migration.CompilationFailsWithoutTestFixturesDependencies()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, ':consumer:compileTestFixturesKotlin')

    then:
    assertThat(result.output).contains("Unresolved reference 'producer'.")
    assertThat(result.output).contains("Unresolved reference 'Person'.")

    where:
    gradleVersion << gradleVersions()
  }
}
