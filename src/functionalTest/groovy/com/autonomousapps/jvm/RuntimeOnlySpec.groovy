package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ImplRuntimeTestImplConfusionProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RuntimeOnlySpec extends AbstractJvmSpec {

  def "don't suggest implementation to runtimeOnly when used for testImplementation (#gradleVersion)"() {
    given:
    def project = new ImplRuntimeTestImplConfusionProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', ImplRuntimeTestImplConfusionProject.SPARK_SQL_ID,
    )

    then: 'advice is correct'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and: 'reason makes sense'
    def output = Colors.decolorize(result.output)
    assertThat(output).contains('You have been advised to change this dependency to \'runtimeOnly\' from \'implementation\'.')
    assertThat(output).contains('There is no advice regarding this dependency.')

    where:
    gradleVersion << gradleVersions()
  }
}
