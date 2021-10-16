package com.autonomousapps.android

import com.autonomousapps.android.projects.DoubleDeclarationsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DoubleDeclarationSpec extends AbstractAndroidSpec {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "doesn't advise to move to api if on api and implementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DoubleDeclarationsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.expectedAdvice).containsExactlyElementsIn(project.actualAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
