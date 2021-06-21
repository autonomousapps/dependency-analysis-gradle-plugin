package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject
import com.autonomousapps.internal.android.AgpVersion
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AndroidTestDependenciesSpec extends AbstractAndroidSpec {

  def "configuration succeeds when a unit test variant is disabled (#gradleVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.Configurable(agpVersion)
    gradleProject = project.gradleProject

    // TODO even "help" should fail, right?
    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << [GradleVersion.version('7.1')]
    agpVersion << [AgpVersion.version('4.2.1').version]
//    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "transitive test dependencies should be declared on testImplementation (#gradleVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    // TODO even "help" should fail, right?
    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << [GradleVersion.version('7.1')]
    agpVersion << [AgpVersion.version('4.2.1').version]
//    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
