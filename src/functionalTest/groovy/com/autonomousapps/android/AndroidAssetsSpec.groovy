package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidAssetsProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

/** See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/657. */
final class AndroidAssetsSpec extends AbstractAndroidSpec {

  def "does not recommend removing dependency on assets-containing modules (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidAssetsProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyDependencyAdviceIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
