package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BundleKmpProject
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

  def "kmp deps form implicit bundles (#gradleVersion)"() {
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
}
