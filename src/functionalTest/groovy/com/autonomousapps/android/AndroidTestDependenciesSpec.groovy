package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AndroidTestDependenciesSpec extends AbstractAndroidSpec {

  def "buildHealth succeeds when a unit test variant is disabled (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.Buildable(agpVersion)
    gradleProject = project.gradleProject

    expect: 'The `adviceRelease` task does not fail due to missing `testCompileGraph` input'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    where:
    gradleVersion << [GRADLE_7_2]
    agpVersion << [AGP_4_2.version]
  }

  def "transitive test dependencies should be declared on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
