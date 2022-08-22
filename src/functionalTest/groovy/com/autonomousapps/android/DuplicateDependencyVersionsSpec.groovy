package com.autonomousapps.android

import com.autonomousapps.android.projects.DuplicateDependencyVersionsProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.kit.truth.BuildResultSubject.buildResults
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class DuplicateDependencyVersionsSpec extends AbstractAndroidSpec {

  def "can show duplicate dependency versions (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DuplicateDependencyVersionsProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion, gradleProject.rootDir, 'printDuplicateDependencies')

    then: 'app resolved dependencies'
    assertThat(project.actualResolvedDependenciesFor('app'))
      .containsExactlyElementsIn(project.expectedResolvedDependenciesForApp)
      .inOrder()

    and: 'lib1 resolved dependencies'
    assertThat(project.actualResolvedDependenciesFor('lib1'))
      .containsExactlyElementsIn(project.expectedResolvedDependenciesForLib1)
      .inOrder()

    and: 'lib2 resolved dependencies'
    assertThat(project.actualResolvedDependenciesFor('lib2'))
      .containsExactlyElementsIn(project.expectedResolvedDependenciesForLib2)
      .inOrder()

    and: 'duplicates report'
    def report = project.actualDuplicateDependencies()
    assertThat(report['junit:junit']).containsExactlyElementsIn('4.11', '4.12', '4.13').inOrder()

    and: 'output'
    assertAbout(buildResults())
      .that(result)
      .output()
      .contains(project.expectedOutput)

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }
}
