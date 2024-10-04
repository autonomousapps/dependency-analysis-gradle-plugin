package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AnnotationsCompileOnlyProject
import com.autonomousapps.jvm.projects.AnnotationsImplementationProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AnnotationsImplementationSpec extends AbstractJvmSpec {

  def "classes used in runtime-retained annotations are implementation (#gradleVersion)"() {
    given:
    def project = new AnnotationsImplementationProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "classes used in compile-retained annotations are compileOnly (#gradleVersion)"() {
    given:
    def project = new AnnotationsCompileOnlyProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')
    // TODO(tsr): still need better tests for reason. Before the fix, this output was wrong. Still not fixed really.
    //, ':consumer:reason', '--id', 'org.jetbrains:annotations')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
