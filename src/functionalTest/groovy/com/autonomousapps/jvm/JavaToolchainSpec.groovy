package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.JavaToolchainProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class JavaToolchainSpec extends AbstractJvmSpec {

  def "does not fail with Java #javaToolchainVersion toolchain (#gradleVersion)"() {
    given: "a Gradle project using Java toolchain version #javaToolchainVersion that has been built"
    def project = new JavaToolchainProject(javaToolchainVersion)
    gradleProject = project.gradleProject
    build(gradleVersion, gradleProject.rootDir, 'build')

    when: 'running buildHealth'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'abiAnalysisMain and explodeByteCodeSourceMain tasks were ran'
    // These two are the ones that were actually failing, sanity check that java library plugin is applied
    assert result.tasks.any { (it.getPath() == ':proj:abiAnalysisMain') }
    assert result.tasks.any { (it.getPath() == ':proj:explodeByteCodeSourceMain') }

    and: 'the report should be empty'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    // Comment out all but the last for speed
    // See https://docs.gradle.org/8.5/userguide/compatibility.html#java
    gradleVersion | javaToolchainVersion
//    GRADLE_7_5    | 18
//    GRADLE_7_5    | 19
//    GRADLE_7_6    | 18
    GRADLE_7_6    | 19
    // TODO(tsr): some errors running these two scenarios
//    GRADLE_8_3    | 20
//    GRADLE_8_5    | 21

    classFileMajorVersion = javaToolchainVersion + 44 // 19 + 44 = 63, is this safe?
  }
}
