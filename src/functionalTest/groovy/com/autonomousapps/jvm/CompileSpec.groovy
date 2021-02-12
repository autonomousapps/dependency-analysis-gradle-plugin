package com.autonomousapps.jvm


import com.autonomousapps.jvm.projects.CompileProject
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CompileSpec extends AbstractJvmSpec {

  @Unroll
  def "compile conf is correctly accounted for (#gradleVersion)"() {
    given:
    def project = new CompileProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions().tap {
      it.removeAll {
        // This test is on a project that uses the `compile` configuration, removed in Gradle 7
        it.baseVersion >= GradleVersion.version('7.0').baseVersion
      }
    }
  }
}
