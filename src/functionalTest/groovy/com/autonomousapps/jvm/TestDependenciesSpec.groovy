package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TestDependenciesProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestDependenciesSpec extends AbstractJvmSpec {

  def "unused test dependencies are reported (#gradleVersion)"() {
    given:
    def project = new TestDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where: 'Spring Boot requires Gradle 6.3+'
    gradleVersion << [GradleVersion.current()]//gradleVersions()
  }
}
