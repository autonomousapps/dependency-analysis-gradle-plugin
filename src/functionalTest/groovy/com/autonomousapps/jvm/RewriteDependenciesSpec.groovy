package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RewriteDependenciesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RewriteDependenciesSpec extends AbstractJvmSpec {

  def "can rewrite dependencies (#gradleVersion)"() {
    given:
    def project = new RewriteDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'proj:fixDependencies')

    then:
    assertThat(project.actualProjectAdvice().dependencyAdvice).containsExactlyElementsIn(
      project.expectedProjectAdvice.dependencyAdvice
    )
    assertThat(trimmedLinesOf(project.projBuildFile().text))
      .containsExactlyElementsIn(trimmedLinesOf(project.expectedBuildFile))
      .inOrder()

    where:
    gradleVersion << [gradleVersions().last()]
  }

  def "can rewrite upgrade dependencies only (#gradleVersion)"() {
    given:
    def project = new RewriteDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'proj:fixDependencies', '--upgrade')

    then:
    assertThat(project.actualProjectAdvice().dependencyAdvice).containsExactlyElementsIn(
      project.expectedProjectAdvice.dependencyAdvice
    )
    assertThat(trimmedLinesOf(project.projBuildFile().text))
      .containsExactlyElementsIn(trimmedLinesOf(project.expectedBuildFileUpgraded))
      .inOrder()

    where:
    gradleVersion << [gradleVersions().last()]
  }

  private static List<String> trimmedLinesOf(CharSequence content) {
    // to lines and trim whitespace off end
    return content.readLines().collect { it.replaceFirst('\\s+\$', '') }
  }
}
