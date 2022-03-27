package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidTestDependenciesProject
import com.autonomousapps.utils.Colors
import org.gradle.testkit.runner.BuildResult

import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ReasonSpec extends AbstractAndroidSpec {

  def "can discover the reason for removing okhttp (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'proj:reason', '--id', 'com.squareup.okhttp3:okhttp:4.6.0')

    then:
    outputMatchesForOkhttp(result)

    when:
    result = build(gradleVersion, gradleProject.rootDir, 'proj:reason', '--id', 'com.squareup.okhttp3:okhttp')

    then:
    outputMatchesForOkhttp(result)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can discover the reason for adding okio (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'proj:reason', '--id', 'com.squareup.okio:okio:2.6.0')

    then:
    outputMatchesForOkio(result)

    when:
    result = build(gradleVersion, gradleProject.rootDir, 'proj:reason', '--id', 'com.squareup.okio:okio')

    then:
    outputMatchesForOkio(result)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "fails when asking about non-existent dependency (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTestDependenciesProject.UsedTransitive(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(
      gradleVersion,
      gradleProject.rootDir,
      'proj:reason', '--id', ':life:the-universe:and-everything'
    )

    then:
    assertAbout(buildTasks()).that(result.task(':proj:reason')).failed()
    assertThat(result.output)
      .contains("There is no dependency with coordinates ':life:the-universe:and-everything' in this project.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  private static void outputMatchesForOkhttp(BuildResult result) {
    def lines = Colors.decolorize(result.output).readLines()
    def asked = lines.find { it.startsWith("You asked about") }
    def advised = lines.find { it.startsWith('You have been advised') }

    assertThat(asked).isEqualTo("You asked about the dependency 'com.squareup.okhttp3:okhttp:4.6.0'.")
    assertThat(advised).isEqualTo("You have been advised to remove this dependency from 'testImplementation'.")

    assertThat(result.output).contains('Source: debug, main')
    assertThat(result.output).contains('Source: release, main')
    assertThat(result.output).contains('Source: debug, test')
    assertThat(result.output).contains('Source: release, test')
    assertThat(lines.findAll { it == '(no usages)' }.size()).isEqualTo(4)
  }

  private static void outputMatchesForOkio(BuildResult result) {
    def lines = Colors.decolorize(result.output).readLines()
    def asked = lines.find { it.startsWith("You asked about") }
    def advised = lines.find { it.startsWith('You have been advised') }

    assertThat(asked).isEqualTo("You asked about the dependency 'com.squareup.okio:okio:2.6.0'.")
    assertThat(advised).isEqualTo("You have been advised to add this dependency to 'testImplementation'.")

    assertThat(result.output).contains('Source: debug, main')
    assertThat(result.output).contains('Source: release, main')
    assertThat(result.output).contains('Source: debug, test')
    assertThat(result.output).contains('Source: release, test')

    assertThat(lines.findAll { it.endsWith('Uses class okio.Buffer (implies testImplementation).') }.size())
      .isEqualTo(2)
    assertThat(lines.findAll { it == '(no usages)' }.size()).isEqualTo(2)
  }
}
