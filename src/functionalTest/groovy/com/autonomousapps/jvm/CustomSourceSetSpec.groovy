package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FeatureVariantTestProject
import com.autonomousapps.jvm.projects.IntegrationTestProject
import com.autonomousapps.jvm.projects.TestFixturesTestProject

import static com.autonomousapps.utils.Runner.build
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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, producerCodeInFeature, additionalCapabilities] << multivariableDataPipe(
      gradleVersions(), [true, false], [true, false])
  }

  def "dependencies to test fixtures (#gradleVersion)"() {
    given:
    def project = new TestFixturesTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
