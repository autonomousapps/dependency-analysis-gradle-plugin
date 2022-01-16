package com.autonomousapps.jvm

import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.jvm.projects.PostProcessingProject2
import com.autonomousapps.jvm.projects.PostProcessingProject3
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PostProcessingSpec extends AbstractJvmSpec {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  // TODO V2: Delete. The new spec is better.
  @IgnoreIf({ PreconditionContext it -> it.sys.v == '2' })
  def "can post-process root project output (#gradleVersion)"() {
    given:
    javaLibraryProject = new PostProcessingProject()

    when:
    def result = build(gradleVersion, javaLibraryProject, ':postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':generateAdviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }

  // TODO V2: Delete. The new spec is better.
  @IgnoreIf({ PreconditionContext it -> it.sys.v == '2' })
  def "can post-process subproject output (#gradleVersion)"() {
    given:
    def project = new PostProcessingProject2()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':proj-1:postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:generateAdviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }

  def "can post-process advice with abstract task (#gradleVersion isV1=#isV1)"() {
    given:
    def project = new PostProcessingProject3(isV1 as Boolean)
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      ':proj-1:postProcess', isV1 ? '-Dv=1' : '-Dv=2'
    )

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS
    assertThat(result.output).contains(isV1 ? 'ComprehensiveAdvice' : 'ProjectAdvice')

    where:
    [gradleVersion, isV1] << multivariableDataPipe(gradleVersions(), [true, false])
  }
}
