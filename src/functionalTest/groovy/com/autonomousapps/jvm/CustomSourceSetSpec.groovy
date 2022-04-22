package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FeatureVariantTestProject
import com.autonomousapps.jvm.projects.IntegrationTestProject
import com.autonomousapps.jvm.projects.TestFixturesTestProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CustomSourceSetSpec extends AbstractJvmSpec {

  // Not yet implemented: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/451
  def "transitive dependency for main but declared on custom source set should not prevent advice (#gradleVersion)"() {
    given:
    def project = new IntegrationTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  // Not yet implemented: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/451
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

  // Not yet implemented: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/298
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
