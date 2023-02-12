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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth(true))

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
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth(false))

    where:
    gradleVersion << gradleVersions()
  }

  def "dependencies for feature variant do not produce any advice (#gradleVersion)"() {
    given:
    def project = new FeatureVariantTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "dependencies for test fixtures do not produce any advice (#gradleVersion)"() {
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
