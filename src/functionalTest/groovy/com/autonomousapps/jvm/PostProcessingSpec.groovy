package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.PostProcessingProject3
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PostProcessingSpec extends AbstractJvmSpec {

  def "can post-process advice with abstract task (#gradleVersion)"() {
    given:
    def project = new PostProcessingProject3()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion, gradleProject.rootDir, ':proj-1:postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS
    assertThat(result.output).contains('ProjectAdvice')

    where:
    gradleVersion << gradleVersions()
  }
}
