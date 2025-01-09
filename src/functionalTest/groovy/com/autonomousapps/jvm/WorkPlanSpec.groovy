package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.WorkPlanProject
import spock.lang.PendingFeature

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class WorkPlanSpec extends AbstractJvmSpec {

  @PendingFeature
  def "can generate work plan (#gradleVersion)"() {
    given:
    def project = new WorkPlanProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':generateWorkPlan')

    then:
    assertThat(true).isFalse()

    where:
    gradleVersion << gradleVersions()
  }
}
