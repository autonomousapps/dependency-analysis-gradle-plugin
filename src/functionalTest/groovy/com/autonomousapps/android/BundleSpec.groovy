package com.autonomousapps.android

import com.autonomousapps.android.projects.BundleProject
import com.autonomousapps.android.projects.KmpAndroidProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class BundleSpec extends AbstractAndroidSpec {

  def "don't advise changing parents with used children (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new BundleProject(agpVersion as String)
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

  def "doesn't suggest adding kmp facade when it's already declared (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KmpAndroidProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '-Pdependency.analysis.print.build.health=true'
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
