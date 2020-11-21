package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AnnotationsAbiProject
import com.autonomousapps.jvm.projects.AnnotationsAbiProject.Target
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class AnnotationsAbiSpec extends AbstractJvmSpec {
  @Unroll
  def "annotations on public classes are part of the abi (#gradleVersion)"() {
    given:
    def project = new AnnotationsAbiProject(Target.CLASS)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "annotations on public methods are part of the abi (#gradleVersion)"() {
    given:
    def project = new AnnotationsAbiProject(Target.METHOD)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "annotations on parameters of public methods are part of the abi (#gradleVersion)"() {
    given:
    def project = new AnnotationsAbiProject(Target.PARAMETER)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
