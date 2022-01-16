package com.autonomousapps.android

import com.autonomousapps.android.projects.DoubleDeclarationsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DoubleDeclarationSpec extends AbstractAndroidSpec {

  // nb: this passes in v2 thanks to the idea that, if a dependency is in a declared bundle, it is its own parent/child.
  @SuppressWarnings('GroovyAssignabilityCheck')
  def "doesn't advise to move to api if on api and implementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DoubleDeclarationsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
