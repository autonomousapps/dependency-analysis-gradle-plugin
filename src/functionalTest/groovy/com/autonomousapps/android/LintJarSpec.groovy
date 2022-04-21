package com.autonomousapps.android

import com.autonomousapps.AdviceHelper
import com.autonomousapps.android.projects.LintJarProject
import com.autonomousapps.android.projects.TimberProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class LintJarSpec extends AbstractAndroidSpec {

  def "do not recommend removing rxlint (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new LintJarProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(AdviceHelper.actualBuildHealth(gradleProject))
      .containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "do recommend removing timber if it is unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TimberProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(AdviceHelper.actualBuildHealth(gradleProject))
      .containsExactlyElementsIn([
        TimberProject.removeTimberAdvice(),
      ])

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
