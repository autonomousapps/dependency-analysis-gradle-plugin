package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RippleProject
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RippleSpec extends AbstractJvmSpec {
  @Unroll
  def "ripples account for transitive dependencies (#gradleVersion)"() {
    given:
    def project = new RippleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':ripples', '--id', ':b')

    then:
    assertThat(actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)
    assertThat(actualRipples().ripples).containsExactlyElementsIn(project.expectedRipplesFromB)

    where:
    gradleVersion << gradleVersions()
  }
}
