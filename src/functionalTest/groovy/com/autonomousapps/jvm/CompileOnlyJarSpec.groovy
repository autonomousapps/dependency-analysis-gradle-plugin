package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.CompileOnlyJarProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CompileOnlyJarSpec extends AbstractJvmSpec {

  def "compileOnly file dependency should not be marked as transitive (#gradleVersion)"() {
    given:
    def project = new CompileOnlyJarProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':external:jar')
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
