package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.PostProcessingProject
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion

import static com.autonomousapps.kit.truth.TestKitTruth.assertThat
import static com.autonomousapps.utils.Runner.build

final class PostProcessingSpec extends AbstractJvmSpec {

  def "can post-process advice with abstract task (#gradleVersion)"() {
    given:
    def project = new PostProcessingProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion, gradleProject.rootDir, ':proj-1:postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS
    assertThat(result).output().contains('ProjectAdvice')

    where:
    gradleVersion << gradleVersions()
  }
}
