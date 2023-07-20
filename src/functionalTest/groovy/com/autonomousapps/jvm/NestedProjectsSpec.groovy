package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.NestedSubprojectsProject

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class NestedProjectsSpec extends AbstractJvmSpec {

  def "handles projects with same name but different nested path"() {
    given:
    def project = new NestedSubprojectsProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "does not handle projects with same name and group but different nested path"() {
    // Gradle does not allow this. As soon group+name of a project are identical they have the same identity from
    // Gradle's perspective and that leads to errors during dependency resolution already.
    // This test is here to document that.

    given:
    def project = new NestedSubprojectsProject(true)
    gradleProject = project.gradleProject

    when: 'building with several projects with the same identity'
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'build fails with circular dependency issue'
    result.output.contains '''
      Circular dependency between the following tasks:
      :featureC:public:compileJava
      \\--- :featureC:public:compileJava (*)'''.stripIndent()

    where:
    gradleVersion << gradleVersions()
  }
}
