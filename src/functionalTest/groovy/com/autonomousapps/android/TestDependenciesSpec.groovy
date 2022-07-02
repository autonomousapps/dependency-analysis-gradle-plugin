package com.autonomousapps.android

import com.autonomousapps.Flags
import com.autonomousapps.android.projects.TestDependenciesProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class TestDependenciesSpec extends AbstractAndroidSpec {

  def "don't advise removing test declarations when test analysis is disabled (#gradleVersion AGP #agpVersion analyzeTests=#analyzeTests)"() {
    given:
    def project = new TestDependenciesProject(agpVersion as String, analyzeTests as Boolean)
    gradleProject = project.gradleProject

    when:
    def testFlag = "-D${Flags.FLAG_TEST_ANALYSIS}=$analyzeTests"
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth', testFlag)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion, analyzeTests] << gradleAgpMatrixPlus([true, false])
  }
}
