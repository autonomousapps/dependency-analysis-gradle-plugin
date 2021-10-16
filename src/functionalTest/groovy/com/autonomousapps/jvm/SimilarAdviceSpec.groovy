package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SimilarAdviceProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SimilarAdviceSpec extends AbstractJvmSpec {

  // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/386
  def "comprehensive advice is specific to a project (#gradleVersion)"() {
    given:
    def project = new SimilarAdviceProject()
    gradleProject = project.gradleProject

    when: 'We run the same tasks on two different projects, with build cache enabled'
    build(gradleVersion, gradleProject.rootDir, 'lib1:aggregateAdvice', '--build-cache')
    build(gradleVersion, gradleProject.rootDir, 'lib2:aggregateAdvice', '--build-cache')

    then: 'The advice for those two projects is uniquely identifiable'
    assertThat(actualComprehensiveAdvice("lib1").projectPath).isEqualTo(':lib1')
    assertThat(actualComprehensiveAdvice("lib2").projectPath).isEqualTo(':lib2')

    where:
    gradleVersion << gradleVersions()
  }
}
