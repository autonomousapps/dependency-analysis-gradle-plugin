package com.autonomousapps.android

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.fixtures.PostProcessingProject2
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.fixtures.jvm.JvmProject
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

class PostProcessingSpec extends AbstractFunctionalSpec {

  private ProjectDirProvider javaLibraryProject = null
  private JvmProject jvmProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
    if (jvmProject != null) {
      clean(jvmProject.rootDir)
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
    jvmProject = new PostProcessingProject2().jvmProject

    when:
    def result = build(gradleVersion, jvmProject.rootDir, ':proj-1:postProcess')

    then: 'The advice task executes (task dependencies work)'
    result.task(':proj-1:adviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':proj-1:postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }
}
