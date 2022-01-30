package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.PostProcessingProject3
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PostProcessingSpec extends AbstractJvmSpec {

  def "can post-process advice with abstract task (#gradleVersion isV1=#isV1)"() {
    given:
    def project = new PostProcessingProject3(isV1 as Boolean)
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      ':proj-1:postProcess', isV1 ? '-Ddependency.analysis.old.model=true' : '-Ddependency.analysis.old.model=false'
    )

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS
    assertThat(result.output).contains(isV1 ? 'ComprehensiveAdvice' : 'ProjectAdvice')

    where:
    [gradleVersion, isV1] << multivariableDataPipe(gradleVersions(), [true, false])
  }
}
