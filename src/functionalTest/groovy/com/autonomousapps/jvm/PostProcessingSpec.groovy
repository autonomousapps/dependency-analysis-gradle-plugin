package com.autonomousapps.jvm

import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.jvm.projects.PostProcessingProject2
import org.gradle.testkit.runner.TaskOutcome
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.PendingFeatureIf

import static com.autonomousapps.utils.Runner.build

// TODO V2: support has not yet been added to v2
final class PostProcessingSpec extends AbstractJvmSpec {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
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

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
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
}
