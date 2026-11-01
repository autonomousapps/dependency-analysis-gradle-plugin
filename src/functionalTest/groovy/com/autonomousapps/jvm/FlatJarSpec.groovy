package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FlatJarBundleProject
import com.autonomousapps.model.Advice
import groovy.json.JsonSlurper

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FlatJarSpec extends AbstractJvmSpec {

  def "transitive file-tree jars are analyzed individually (#gradleVersion)"() {
    given:
    def project = new FlatJarBundleProject()
    gradleProject = project.gradleProject
    build(gradleVersion, gradleProject.rootDir, ':first:jar', ':second:jar')
    project.copyJars()

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualProjectAdviceForProject(gradleProject, 'provider').dependencyAdvice).containsExactly(
      Advice.ofRemove(flatCoordinates('first.jar'), 'api'),
      Advice.ofRemove(flatCoordinates('second.jar'), 'api'),
    )
    assertThat(actualProjectAdviceForProject(gradleProject, 'consumer').dependencyAdvice).containsExactly(
      Advice.ofRemove(projectCoordinates(':provider'), 'implementation'),
    )
    def usages = new JsonSlurper().parse(new File(gradleProject.rootDir,
      'consumer/build/reports/dependency-analysis/usages-dependencies.json')).usages
    assertThat(usages.findAll { it.coordinates.type == 'flat' && it.usages.any { it.bucket == 'IMPL' } }
      .collect { it.coordinates.identifier }).containsExactly('first.jar', 'second.jar')

    where:
    gradleVersion << gradleVersions()
  }

  def "runtime file-tree jars do not leak into compile graph (#gradleVersion)"() {
    given:
    def project = new FlatJarBundleProject(false, false, 'runtimeOnly')
    gradleProject = project.gradleProject
    build(gradleVersion, gradleProject.rootDir, ':first:jar', ':second:jar')
    project.copyJars()

    when:
    build(gradleVersion, gradleProject.rootDir, ':consumer:graphViewMain')

    then:
    def reports = new File(gradleProject.rootDir, 'consumer/build/reports/dependency-analysis/main/graph')
    def compile = new JsonSlurper().parse(new File(reports, 'graph-compile.json'))
    def runtime = new JsonSlurper().parse(new File(reports, 'graph-runtime.json'))
    assertThat(compile.graphJson.nodes.findAll { it.type == 'flat' }).isEmpty()
    assertThat(runtime.graphJson.nodes.findAll { it.type == 'flat' }*.identifier)
      .containsExactly('first.jar', 'second.jar')

    where:
    gradleVersion << gradleVersions()
  }
}
