package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BundleKmpProject
import com.autonomousapps.jvm.projects.BundleKmpProject2
import com.autonomousapps.jvm.projects.BundleProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class BundleSpec extends AbstractJvmSpec {

  def "can define entry point to bundle (#gradleVersion)"() {
    given:
    def project = new BundleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "kmp deps form implicit bundles when kmp dep is declared (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  def "kmp deps form implicit bundles when none are declared (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject2()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    // TODO reason needs to be updated to show that the -jvm variant is used
    when:
    build(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:okio')

    then:
    true

    where:
    gradleVersion << [gradleVersions().last()]
  }
}
