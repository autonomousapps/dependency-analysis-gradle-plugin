package com.autonomousapps.android

import com.autonomousapps.android.projects.DefaultAndroidProject
import com.autonomousapps.fixtures.JavaOnlyAndroidProject
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class OtherAndroidSpec extends AbstractAndroidSpec {

  def "can configure java-only app module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new JavaOnlyAndroidProject(agpVersion)
    androidProject = project.newProject()

    expect:
    build(gradleVersion, androidProject, 'buildHealth')

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "buildHealth can be executed (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DefaultAndroidProject(agpVersion)
    androidProject = project.newProject()

    when:
    def result = build(gradleVersion, androidProject, 'buildHealth')

    then: 'expected tasks ran in root project'
    result.task(':adviceReport').outcome == TaskOutcome.SUCCESS
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in app project'
    result.task(':app:misusedDependenciesDebug').outcome == TaskOutcome.SUCCESS
    result.task(':app:generateAdviceDebug').outcome == TaskOutcome.SUCCESS

    and: 'expected tasks ran in lib project'
    result.task(':lib:misusedDependenciesDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib:abiAnalysisDebug').outcome == TaskOutcome.SUCCESS
    result.task(':lib:generateAdviceDebug').outcome == TaskOutcome.SUCCESS

    and: 'unused dependencies reports for app are correct'
    def actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor('app')
    def expectedUnusedDepsForApp = [
      ':java_lib', ':lib', ':kotlin_lib',
      'androidx.constraintlayout:constraintlayout',
      'com.google.android.material:material'
    ]
    assertThat(actualUnusedDepsForApp).containsExactlyElementsIn(expectedUnusedDepsForApp)

    and: 'unused dependencies reports for lib are correct'
    def actualUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor("lib")
    def expectedUnusedDepsForLib = ['androidx.constraintlayout:constraintlayout']
    assertThat(actualUnusedDepsForLib).containsExactlyElementsIn(expectedUnusedDepsForLib)

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor('lib')
    def expectedAbi = ['androidx.core:core', 'org.jetbrains.kotlin:kotlin-stdlib']
    assertThat(actualAbi).containsExactlyElementsIn(expectedAbi)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
