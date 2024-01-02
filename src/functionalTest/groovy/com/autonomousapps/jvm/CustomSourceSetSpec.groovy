// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0

//file:noinspection GroovyAssignabilityCheck
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.*
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.jvm.projects.SourceSetFilteringProject.Severity.*
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class CustomSourceSetSpec extends AbstractJvmSpec {

  def "transitive dependency for main but declared on custom source set should not prevent advice (#gradleVersion)"() {
    given:
    def project = new IntegrationTestProject(true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "advice for custom test source set dependencies pointing to main variant of other component (#gradleVersion)"() {
    given:
    def project = new IntegrationTestProject(false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "dependencies to feature variants (#gradleVersion producerCodeInFeature=#producerCodeInFeature additionalCapabilities=#additionalCapabilities)"() {
    given:
    def project = new FeatureVariantTestProject(producerCodeInFeature, additionalCapabilities)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, producerCodeInFeature, additionalCapabilities] << multivariableDataPipe(
      gradleVersionsCC(), [true, false], [true, false])
  }

  def "dependencies to test fixtures (#gradleVersion)"() {
    given:
    def project = new TestFixturesTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "dependencies to main and test fixtures of another project are analysed correctly (#gradleVersion nestedProjects=#withNestedProjects)"() {
    given:
    def project = new TestFixturesTestProject2(withNestedProjects)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, withNestedProjects] << multivariableDataPipe(gradleVersions(), [true, false])
  }

  def "dependencies to different variants of the same project are analysed correctly (#gradleVersion)"() {
    given:
    def project = new FeatureVariantInSameProjectTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "test fixtures analysis can be deactivated (#gradleVersion)"() {
    given:
    def project = new TestFixturesTestProject(true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "custom source set analysis can be deactivated (#gradleVersion)"() {
    given:
    def project = new FeatureVariantTestProject(true, false, true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersionsCC()
  }

  def "custom source set analysis can be fine-filtered for severity=#severity (#gradleVersion)"() {
    given:
    def project = new SourceSetFilteringProject.Filtering(severity)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, severity] << multivariableDataPipe(
      gradleVersions(),
      [IGNORE, WARN]
    )
  }

  def "custom source set analysis can be layered: severity=#severity (#gradleVersion)"() {
    given:
    def project = new SourceSetFilteringProject.Layering(severity)
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, severity] << multivariableDataPipe(
      gradleVersions(),
      [IGNORE, FAIL]
    )
  }

  // This validates logic in StandardTransform.simplify() that handles redundant declarations as well as preventing
  // upgrading test dependencies.
  def "don't suggest redundant declarations in related source sets, nor upgrade test dependencies (#gradleVersion #sourceType)"() {
    given:
    def project = new CustomTestSourceSetProject(sourceType)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, sourceType] << multivariableDataPipe(
      gradleVersions(),
      [SourceType.KOTLIN, SourceType.JAVA]
    )
  }
}
