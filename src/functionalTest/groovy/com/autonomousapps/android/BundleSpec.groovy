package com.autonomousapps.android

import com.autonomousapps.android.projects.BundleProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class BundleSpec extends AbstractAndroidSpec {

  def "don't advise changing parents with used children (#gradleVersion)"() {
    given:
    def project = new BundleProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
