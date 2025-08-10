// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.IncludedBuildProject
import com.autonomousapps.jvm.projects.IncludedBuildWithAnnotationProcessorProject
import com.autonomousapps.jvm.projects.IncludedBuildWithSubprojectsProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/565
final class IncludedBuildSpec extends AbstractJvmSpec {

  /**
   * Only since Gradle 8.2 we can use the more precise BuildIdentifier.buildPath (instead of BuildIdentifier.name).
   * That's why the expectations in this test differ depending on Gradle version.
   */
  private final static isAtLeastGradle82(GradleVersion version)  {
    version >= GradleVersion.version("8.2")
  }

  def "doesn't crash in presence of an included build (#gradleVersion)"() {
    given:
    def project = new IncludedBuildProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then: 'the build health of the first build is as expected'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth(isAtLeastGradle82(gradleVersion) ? ':second-build' : 'second-build'))

    and: 'the build health of the second build is as expected'
    assertThat(project.actualBuildHealthOfSecondBuild())
      .containsExactlyElementsIn(project.expectedBuildHealthOfIncludedBuild(':'))

    where: 'This new feature only works for Gradle 7.3+'
    gradleVersion << gradleVersions()
  }

  def "result of analysis does not change if root build of included build tree changes for projects without subprojects"() {
    given:
    def project = new IncludedBuildProject()
    gradleProject = project.gradleProject

    when: 'build running from second build root does not fail'
    build(gradleVersion, new File(gradleProject.rootDir, 'second-build'), ':the-project:buildHealth')
    build(gradleVersion, new File(gradleProject.rootDir, 'second-build'), ':buildHealth')

    then: 'the build health of the first build is the same as when running that build directly'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth(':'))

    then: 'the build health of the second build is the same as when running that build as included build'
    assertThat(project.actualBuildHealthOfSecondBuild())
      .containsExactlyElementsIn(project.expectedBuildHealthOfIncludedBuild(isAtLeastGradle82(gradleVersion) ? ':the-project' : 'the-project'))

    where:
    gradleVersion << gradleVersions()
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
    gradleVersion << gradleVersions()
  }

  def "does not confuse identities of included subprojects depending on each other by GA dependency notation"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualIncludedBuildHealth())
      .containsExactlyElementsIn(project.expectedIncludedBuildHealth(isAtLeastGradle82(gradleVersion) ? ':second-build' : 'second-build'))

    where:
    gradleVersion << gradleVersions()
  }

  def "does not confuse identities of included subprojects depending on each other by project dependency notation"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject(true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then:
    assertThat(project.actualIncludedBuildHealth())
      .containsExactlyElementsIn(project.expectedIncludedBuildHealth(isAtLeastGradle82(gradleVersion) ? ':second-build' : 'second-build'))

    where:
    gradleVersion << gradleVersions()
  }

  def "result of analysis does not change if root build of included build tree changes for projects with subprojects"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject(true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, new File(gradleProject.rootDir, 'second-build'), ':buildHealth')

    then:
    assertThat(project.actualIncludedBuildHealth()).containsExactlyElementsIn(project.expectedIncludedBuildHealth(':'))

    where:
    gradleVersion << gradleVersions()
  }

  def "up-to-date check is correct when switching root builds"() {
    given:
    def project = new IncludedBuildWithSubprojectsProject(true)
    gradleProject = project.gradleProject

    when: 'The first build is the root'
    build(gradleVersion, gradleProject.rootDir, ':second-build:buildHealth')

    then:
    assertThat(project.actualIncludedBuildHealth())
      .containsExactlyElementsIn(project.expectedIncludedBuildHealth(isAtLeastGradle82(gradleVersion) ? ':second-build' : 'second-build'))

    when: 'The second build is the root - the "buildPath" attribute of ProjectCoordinates changes'
    build(gradleVersion, new File(gradleProject.rootDir, 'second-build'), ':buildHealth')

    then:
    assertThat(project.actualIncludedBuildHealth()).containsExactlyElementsIn(project.expectedIncludedBuildHealth(':'))

    where:
    gradleVersion << gradleVersions()
  }

  def "can handle annotation processors from cache in subsequent builds"() {
    given:
    def project = new IncludedBuildWithAnnotationProcessorProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, '--build-cache', ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    when: 'Running again - UP-TO-DATE'
    build(gradleVersion, gradleProject.rootDir, '--build-cache', ':buildHealth')

    then: 'Result is the same'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    when: 'Running again - FROM-CACHE'
    build(gradleVersion, gradleProject.rootDir, '--build-cache', 'clean', ':buildHealth')

    then: 'Result is the same'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
