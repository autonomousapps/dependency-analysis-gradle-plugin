package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidKotlinInlineProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

/**
 * Regression test for
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/173.
 */
final class AndroidKotlinInlineSpec extends AbstractAndroidSpec {

  def "inline usage in a kotlin source set is recognized (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidKotlinInlineProject(agpVersion as String)
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
