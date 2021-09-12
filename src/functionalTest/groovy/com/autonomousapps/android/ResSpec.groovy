package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidResourceProject
import com.autonomousapps.android.projects.AttrResProject
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class ResSpec extends AbstractAndroidSpec {

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

  def "detects attr usage in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AttrResProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.expectedBuildHealth).containsExactlyElementsIn(project.actualBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }
}
