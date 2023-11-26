package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.VersionCatalogProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class VersionCatalogSpec extends AbstractJvmSpec {

  def "version catalogs work (#gradleVersion)"() {
    given:
    def project = new VersionCatalogProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    when: 'We ask about the reason using the version catalog alias'
    def result = build(gradleVersion, gradleProject.rootDir, 'lib:reason', '--id', 'libs.commonCollections')

    then: 'It works'
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        You asked about the dependency 'org.apache.commons:commons-collections4:4.4 (libs.commonCollections)'.
        You have been advised to remove this dependency from 'implementation'.'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }
}
