package com.autonomousapps.android

import com.autonomousapps.AdviceHelper
import com.autonomousapps.android.projects.KaptProject
import com.autonomousapps.fixtures.*

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class AnnotationProcessorSpec extends AbstractAndroidSpec {

  def "kapt is not redundant when there are used procs (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AutoValueProjectUsedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec).first().pluginAdvice
    def expectedAdvice = project.expectedAdviceForRoot
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "kapt is redundant when no procs are used (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KaptIsRedundantWithUnusedProcsProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec)
      .find { it.projectPath == ":app" }
      .pluginAdvice
    def expectedAdvice = project.expectedAdvice
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "kapt is redundant when no procs are declared (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KaptIsRedundantProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec)
      .find { it.projectPath == ":app" }
      .pluginAdvice
    def expectedAdvice = project.expectedAdvice
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "kapt redundancy can be ignored by user (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KaptProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def actualPluginAdvice = AdviceHelper.actualBuildHealth(gradleProject)
      .find { it.projectPath == ':lib' }
      .pluginAdvice
    assertThat(actualPluginAdvice.size()).isEqualTo(0)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "autovalue is used with kapt (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AutoValueProjectUsedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is unused with kapt (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUnusedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is used with kapt on method (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByKaptForMethod(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    assertThat(actualAdvice).containsExactlyElementsIn(project.expectedAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is used with kapt on class (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByKaptForClass(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is unused with annotationProcessor (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUnusedByAnnotationProcessor(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is used with annotationProcessor on method (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByAnnotationProcessorForMethod(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    assertThat(actualAdvice).containsExactlyElementsIn(project.expectedAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "dagger is used with annotationProcessor on class (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByAnnotationProcessorForClass(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
