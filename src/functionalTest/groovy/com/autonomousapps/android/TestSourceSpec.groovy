package com.autonomousapps.android

import com.autonomousapps.android.projects.KotlinTestJunitProject
import com.autonomousapps.android.projects.TestSourceProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class TestSourceSpec extends AbstractAndroidSpec {

  def "test dependencies should be on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TestSourceProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "kotlin-test-junit should be androidTestRuntimeOnly (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KotlinTestJunitProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
