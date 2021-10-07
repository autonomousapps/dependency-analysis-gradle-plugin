package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.EnumOnlyLibProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class EnumOnlyLibSpec extends AbstractJvmSpec {

  def "detects enum-only libraries not marked as compileOnly (#gradleVersion)"() {
    given:
    def project = new EnumOnlyLibProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualAdvice('proj')).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }

}
