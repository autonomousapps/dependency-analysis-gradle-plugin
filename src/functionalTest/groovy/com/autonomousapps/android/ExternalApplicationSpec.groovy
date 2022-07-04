package com.autonomousapps.android

import com.autonomousapps.android.projects.ExternalApplicationProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class ExternalApplicationSpec extends AbstractAndroidSpec {

  def "finds external application class in manifest (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ExternalApplicationProject(agpVersion as String)
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
