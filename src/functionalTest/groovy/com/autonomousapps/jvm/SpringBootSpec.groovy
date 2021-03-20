package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SpringBootProject
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SpringBootSpec extends AbstractJvmSpec {

  @Unroll
  def "does not suggest api dependencies (#gradleVersion)"() {
    given:
    def project = new SpringBootProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where: 'Spring Boot requires Gradle 6.3+'
    gradleVersion << gradleVersions().tap {
      it.removeIf {
        it.baseVersion < GradleVersion.version('6.3').baseVersion
      }
    }
  }
}
