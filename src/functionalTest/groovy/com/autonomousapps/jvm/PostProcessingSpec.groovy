package com.autonomousapps.jvm

import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.jvm.projects.PostProcessingProject2
import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

class PostProcessingSpec extends AbstractJvmSpec {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  @Unroll
  def "can post-process root project output (#gradleVersion)"() {
    given:
    javaLibraryProject = new PostProcessingProject()

    when:
    def result = build(gradleVersion, javaLibraryProject, ':postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':adviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "can post-process subproject output (#gradleVersion)"() {
    given:
    def project = new PostProcessingProject2()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':proj-1:postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:adviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }
}
