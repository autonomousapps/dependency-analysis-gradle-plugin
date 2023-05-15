package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.IncludedBuildProject
import com.autonomousapps.jvm.projects.IncludedBuildWithSubprojectsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/565
final class IncludedBuildSpec extends AbstractJvmSpec {

  private static INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS = [
    GRADLE_7_3, GRADLE_7_4, SUPPORTED_GRADLE_VERSIONS.last()
  ]

  def "doesn't crash in presence of an included build (#gradleVersion)"() {
    given:
    def project = new IncludedBuildProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'This new feature only works for Gradle 7.3+'
    gradleVersion << INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS
  }

  def "does not confuse identities of included subprojects depended on by another build"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS
  }

  def "does not confuse identities of included subprojects depending on each other by GA dependency notation"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualIncludedBuildHealth()).containsExactlyElementsIn(project.expectedIncludedBuildHealth)

    where:
    gradleVersion << INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS
  }

  def "does not confuse identities of included subprojects depending on each other by project dependency notation"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject(true)
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualIncludedBuildHealth()).containsExactlyElementsIn(project.expectedIncludedBuildHealth)

    where:
    gradleVersion << INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS
  }

  def "can handle annotation processors from cache in subsequent builds"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject(true)
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualIncludedBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << INCLUDED_BUILD_SUPPORT_GRADLE_VERSIONS
  }
}
