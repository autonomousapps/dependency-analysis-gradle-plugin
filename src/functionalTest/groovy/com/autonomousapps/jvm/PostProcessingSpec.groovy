package com.autonomousapps.jvm

import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.jvm.projects.PostProcessingProject2
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.utils.Runner.build

final class PostProcessingSpec extends AbstractJvmSpec {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

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
