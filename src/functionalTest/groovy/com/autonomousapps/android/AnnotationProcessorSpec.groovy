package com.autonomousapps.android

import com.autonomousapps.fixtures.*
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build

final class AnnotationProcessorSpec extends AbstractAndroidSpec {

  @Unroll
  def "kapt is not redundant when there are used procs (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AutoValueProjectUsedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec).first().pluginAdvice
    def expectedAdvice = project.expectedAdviceForRoot
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "kapt is redundant when no procs are used (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KaptIsRedundantWithUnusedProcsProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec).first().pluginAdvice
    def expectedAdvice = project.expectedAdvice
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "kapt is redundant when no procs are declared (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new KaptIsRedundantProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.buildHealthFor(project.rootSpec).first().pluginAdvice
    def expectedAdvice = project.expectedAdvice
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "autovalue is used with kapt (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AutoValueProjectUsedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is unused with kapt (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUnusedByKapt(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is used with kapt on method (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByKaptForMethod(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is used with kapt on class (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByKaptForClass(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is unused with annotationProcessor (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUnusedByAnnotationProcessor(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is used with annotationProcessor on method (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByAnnotationProcessorForMethod(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
  def "dagger is used with annotationProcessor on class (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DaggerProjectUsedByAnnotationProcessorForClass(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    expectedAdvice == actualAdvice

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
