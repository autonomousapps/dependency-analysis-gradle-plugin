package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GradleVersionCatalogProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class GradleVersionCatalogSpec extends AbstractJvmSpec {

  def "can write advice with catalog aliases"() {
    given:
    def project = new GradleVersionCatalogProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(false)

    where:
    gradleVersion << gradleVersions()
  }
}
