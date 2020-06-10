package com.autonomousapps.android

import com.autonomousapps.AdviceHelper
import com.autonomousapps.android.projects.TestSourceProject
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestSourceSpec extends AbstractAndroidSpec {

  @Unroll
  def "test dependencies should be on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TestSourceProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(AdviceHelper.actualBuildHealth(gradleProject)).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
