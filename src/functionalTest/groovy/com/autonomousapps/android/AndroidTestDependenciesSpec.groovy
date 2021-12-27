package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.PendingFeatureIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: test support is not yet implemented
@SuppressWarnings("GroovyAssignabilityCheck")
final class AndroidTestDependenciesSpec extends AbstractAndroidSpec {

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
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

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
  def "transitive test dependencies should be declared on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
