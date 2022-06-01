package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.JarTransformingProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ComponentWithMultipleArtifactsSpec extends AbstractJvmSpec {

  def "one component can have multiple Jars (#gradleVersion)"() {
    given:
    def project = new JarTransformingProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
