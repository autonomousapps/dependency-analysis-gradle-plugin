package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AppleSiliconProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AppleSiliconSpec extends AbstractJvmSpec {

  def "detects dylib dep as runtime (#gradleVersion)"() {
    given:
    def project = new AppleSiliconProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', project.sqlite,
    )

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    Colors.decolorize(result.output).contains(
      '''\
      Source: main
      ------------
      * Provides 1 native binary: libsqlite4java-osx-aarch64-1.0.392.dylib (implies runtimeOnly).'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }
}
