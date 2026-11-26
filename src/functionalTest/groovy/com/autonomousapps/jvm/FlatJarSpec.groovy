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

  def "project bundles preserve exported jars and only used facades (#gradleVersion, used=#used, primary=#primary)"() {
    given:
    def project = new FlatJarBundleProject(true, used, 'api', primary)
    gradleProject = project.gradleProject
    build(gradleVersion, gradleProject.rootDir, ':first:jar', ':second:jar')
    project.copyJars()

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')
    def cached = build(gradleVersion, gradleProject.rootDir, 'buildHealth')
    def reason = build(gradleVersion, gradleProject.rootDir, ':provider:reason', '--id', 'first.jar')

    then:
    assertThat(actualProjectAdviceForProject(gradleProject, 'provider').dependencyAdvice).isEmpty()
    assertThat(actualProjectAdviceForProject(gradleProject, 'consumer').dependencyAdvice)
      .containsExactlyElementsIn(used ? [] : [Advice.ofRemove(projectCoordinates(':provider'), 'implementation')])
    assertThat(cached.output).contains('Reusing configuration cache.')
    assertThat(reason.output).contains('as an api file dependency in an explicit')

    where:
    [gradleVersion, used, primary] << gradleVersions().collectMany { version ->
      [true, false].collectMany { used -> [true, false].collect { primary -> [version, used, primary] } }
    }
  }

  def "project bundle does not protect private unused jars (#gradleVersion)"() {
    given:
    def project = new FlatJarBundleProject(true, false, 'implementation')
    gradleProject = project.gradleProject
    build(gradleVersion, gradleProject.rootDir, ':first:jar', ':second:jar')
    project.copyJars()

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(actualProjectAdviceForProject(gradleProject, 'provider').dependencyAdvice).containsExactly(
      Advice.ofRemove(flatCoordinates('first.jar'), 'implementation'),
      Advice.ofRemove(flatCoordinates('second.jar'), 'implementation'),
    )
    assertThat(actualProjectAdviceForProject(gradleProject, 'consumer').dependencyAdvice).containsExactly(
      Advice.ofRemove(projectCoordinates(':provider'), 'implementation'),
    )

    where:
    gradleVersion << gradleVersions()
  }
}
