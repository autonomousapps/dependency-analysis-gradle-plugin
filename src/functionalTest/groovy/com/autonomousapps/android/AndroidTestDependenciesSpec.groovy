package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AndroidTestDependenciesSpec extends AbstractAndroidSpec {

  def "configuration succeeds when a unit test variant is disabled (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.Configurable(agpVersion)
    gradleProject = project.gradleProject

    expect: 'The `tasks` task realizes all tasks, which would trigger the bug'
    build(gradleVersion, gradleProject.rootDir, 'tasks')

    where:
    gradleVersion << [GRADLE_7_1]
    agpVersion << [AGP_4_2.version]
  }

  def "transitive test dependencies should be declared on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
