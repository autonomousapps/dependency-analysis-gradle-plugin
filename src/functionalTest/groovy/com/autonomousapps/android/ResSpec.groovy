package com.autonomousapps.android

import com.autonomousapps.fixtures.AndroidResourceProject
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

@SuppressWarnings("GroovyAssignabilityCheck")
final class ResSpec extends AbstractAndroidSpec {

  @Unroll
  def "plugin accounts for android resource usage (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidResourceProject(agpVersion)
    def androidProject = project.newProject()

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then:
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and:
    def actualUnusedDepsForApp = androidProject.unusedDependenciesFor('app')
    def expectedUnusedDepsForApp = ['org.jetbrains.kotlin:kotlin-stdlib-jdk7']
    expectedUnusedDepsForApp == actualUnusedDepsForApp

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
