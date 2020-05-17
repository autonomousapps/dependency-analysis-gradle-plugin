package com.autonomousapps.android


import com.autonomousapps.android.projects.DefaultAndroidProject
import com.autonomousapps.fixtures.JavaOnlyAndroidProject
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

@SuppressWarnings("GroovyAssignabilityCheck")
final class OtherAndroidSpec extends AbstractAndroidSpec {

  @Unroll
  def "can configure java-only app module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new JavaOnlyAndroidProject(agpVersion)
    androidProject = project.newProject()

    expect:
    build(gradleVersion, androidProject, 'buildHealth')

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "buildHealth can be executed (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DefaultAndroidProject(agpVersion)
    androidProject = project.newProject()

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then: 'expected tasks ran in root project'
    result.task(':abiReport').outcome == TaskOutcome.SUCCESS
    result.task(':misusedDependenciesReport').outcome == TaskOutcome.SUCCESS
    result.task(':adviceReport').outcome == TaskOutcome.SUCCESS
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in app project'
    result.task(":app:misusedDependenciesDebug").outcome == TaskOutcome.SUCCESS
    result.task(":app:adviceDebug").outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in lib project'
    result.task(":lib:misusedDependenciesDebug").outcome == TaskOutcome.SUCCESS
    result.task(":lib:abiAnalysisDebug").outcome == TaskOutcome.SUCCESS
    result.task(":lib:adviceDebug").outcome == TaskOutcome.SUCCESS

    and: 'unused dependencies reports for app are correct'
    def actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor('app')
    def expectedUnusedDepsForApp = [
      'androidx.constraintlayout:constraintlayout',
      'com.google.android.material:material'
    ]
    expectedUnusedDepsForApp == actualUnusedDepsForApp

    and: 'unused dependencies reports for lib are correct'
    def actualUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor("lib")
    def expectedUnusedDepsForLib = ['androidx.constraintlayout:constraintlayout']
    expectedUnusedDepsForLib == actualUnusedDepsForLib

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor('lib')
    def expectedAbi = ['androidx.core:core']
    expectedAbi == actualAbi

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
