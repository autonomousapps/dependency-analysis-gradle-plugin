package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SimpleJvmLibraryProject

import static com.autonomousapps.kit.truth.TestKitTruth.assertThat
import static com.autonomousapps.utils.Runner.build

final class AssembleArchivesSpec extends AbstractJvmSpec {

  def "does not execute advice tasks as part of 'assemble' (#gradleVersion)"() {
    given:
    def project = new SimpleJvmLibraryProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'proj:assemble')

    then: 'only `assemble` ran'
    assertThat(result).task(":proj:aggregateAdvice").isNull()
    assertThat(result.tasks).containsExactlyPathsIn([":proj:assemble"])

    where:
    gradleVersion << gradleVersions()
  }
}
