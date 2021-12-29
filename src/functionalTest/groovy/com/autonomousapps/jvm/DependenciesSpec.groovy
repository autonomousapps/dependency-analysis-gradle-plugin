package com.autonomousapps.jvm

import com.autonomousapps.android.AbstractAndroidSpec
import com.autonomousapps.jvm.projects.KotlinStdlibProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DependenciesSpec extends AbstractAndroidSpec {

  def "kotlin stdlib is a dependency bundle by default (#gradleVersion)"() {
    given:
    def project = new KotlinStdlibProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'advice to change stdlib deps'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedBundleAdvice())

    where:
    gradleVersion << gradleVersions()
  }
}
