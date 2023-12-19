package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FindDeclarationsProject

import static com.autonomousapps.kit.truth.TestKitTruth.assertThat
import static com.autonomousapps.utils.Runner.build

final class FindDeclarationsSpec extends AbstractJvmSpec {

  def "findDeclarations is not up to date when build script changes (#gradleVersion)"() {
    given:
    def project = new FindDeclarationsProject()
    gradleProject = project.gradleProject
    def task = ":${project.name}:findDeclarations"

    when: 'We build the first time'
    def result = build(gradleVersion, gradleProject.rootDir, task)

    then: 'Task was successful'
    assertThat(result).task(task).succeeded()

    when: 'We mutate the build script and build again'
    project.mutateBuildScript()
    result = build(gradleVersion, gradleProject.rootDir, task)

    then: 'Task was not up to date'
    assertThat(result).task(task).succeeded()

    where:
    gradleVersion << gradleVersions()
  }
}
