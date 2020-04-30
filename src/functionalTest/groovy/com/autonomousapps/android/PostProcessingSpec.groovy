package com.autonomousapps.android

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.fixtures.PostProcessingProject
import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

class PostProcessingSpec extends AbstractFunctionalSpec {

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

    then:
    result.task(':adviceMain').outcome == TaskOutcome.SUCCESS
    result.task(':postProcess').outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << gradleVersions()
  }

//  @Unroll
//  def "can post-process subproject output (#gradleVersion)"() {
//    given:
//    javaLibraryProject = new PostProcessingProject()
//
//    when:
//    def result = build(gradleVersion, javaLibraryProject, ':postProcess')
//
//    then:
//    result.task(':adviceMain').outcome == TaskOutcome.SUCCESS
//    result.task(':postProcess').outcome == TaskOutcome.SUCCESS
//
//    where:
//    gradleVersion << gradleVersions()
//  }
}
