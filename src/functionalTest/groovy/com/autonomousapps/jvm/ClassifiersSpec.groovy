package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ClassifierTestProject
import com.autonomousapps.jvm.projects.TransitiveClassifierTestProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ClassifiersSpec extends AbstractJvmSpec {

  def "dependencies with classifier does not lead to wrong advice (#gradleVersion #variant)"() {
    given:
    def project = new ClassifierTestProject(variant)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, variant] << multivariableDataPipe(
      gradleVersions(), ClassifierTestProject.TestProjectVariant.values().toList())
  }

  def "transitive classifier dependencies do not lead to wrong advice (#gradleVersion #variant)"() {
    given:
    def project = new TransitiveClassifierTestProject(variant)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, variant] << multivariableDataPipe(
      gradleVersionsCC(), TransitiveClassifierTestProject.TestProjectVariant.values().toList())
  }
}
