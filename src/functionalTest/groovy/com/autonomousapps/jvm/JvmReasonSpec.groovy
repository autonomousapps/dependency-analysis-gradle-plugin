package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.jvm.projects.NestedSubprojectsProject
import com.autonomousapps.utils.Colors
import org.gradle.testkit.runner.BuildResult

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class JvmReasonSpec extends AbstractFunctionalSpec {

  def "can discover reason for project dependency defined by project path (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = ':featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    outputMatchesForProject(result)

    where:
    gradleVersion << gradleVersions()
  }

  def "can discover reason for project dependency defined by coordinates (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = 'the-project.featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    outputMatchesForProject(result)

    where:
    gradleVersion << gradleVersions()
  }

  private static void outputMatchesForProject(BuildResult result) {
    def lines = Colors.decolorize(result.output).readLines()
    def asked = lines.find { it.startsWith('You asked about') }
    def advised = lines.find { it.startsWith('There is no advice') }

    assertThat(asked).isEqualTo("You asked about the dependency 'the-project.featureA:public'.")
    assertThat(advised).isEqualTo("There is no advice regarding this dependency.")
  }

}
