package com.autonomousapps.android

import com.autonomousapps.android.projects.*
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
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
    def unused = androidProject.adviceFor('app')
      .findAll { it.isRemove() }
      .collect { it.coordinates.identifier }
    assertThat(unused).isEmpty()

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
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }

  def "detects res usage in menu.xml file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidMenuProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "gracefully handles @null in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AttrResWithNullProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }

  def "gracefully handles dataBinding expressions in res files (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingWithExpressionsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AGP_4_2)
  }

  def "detects content reference in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DrawableFileProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "detects theme set in manifest (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThemeProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }
}
