package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class AndroidTestDependenciesSpec extends AbstractAndroidSpec {

  def "buildHealth succeeds when a unit test variant is disabled (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.Buildable(agpVersion)
    gradleProject = project.gradleProject

    expect: 'The `adviceRelease` task does not fail due to missing `testCompileGraph` input'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "transitive test dependencies should be declared on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
