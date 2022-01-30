package com.autonomousapps.android

import com.autonomousapps.FlagsKt
import com.autonomousapps.android.projects.TestDependenciesProject
import org.gradle.util.GradleVersion
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestDependenciesSpec extends AbstractAndroidSpec {

  @IgnoreIf({ PreconditionContext it -> it.sys.'dependency.analysis.old.model' == 'true' })
  def "don't advise removing test declarations when test analysis is disabled (#gradleVersion AGP #agpVersion analyzeTests=#analyzeTests)"() {
    given:
    def project = new TestDependenciesProject(agpVersion as String, analyzeTests as Boolean)
    gradleProject = project.gradleProject

    when:
    def testFlag = "-D${FlagsKt.FLAG_TEST_ANALYSIS}=$analyzeTests"
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth', testFlag)

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion, analyzeTests] << gradleAgpMatrixPlus([true, false])
  }
}
