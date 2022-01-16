package com.autonomousapps.android

import com.autonomousapps.android.projects.DefaultAndroidProject
import com.autonomousapps.fixtures.JavaOnlyAndroidProject

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
    build(gradleVersion, androidProject, 'buildHealth')

    then: 'unused dependencies reports for app are correct'
    def appUnused = androidProject.adviceFor('app')
      .findAll { it.isRemove() }
      .collect { it.dependency.identifier }
    assertThat(appUnused).containsExactlyElementsIn([
      ':kotlin_lib',
      ':lib',
      'androidx.constraintlayout:constraintlayout',
      'androidx.core:core-ktx',
      'androidx.navigation:navigation-fragment-ktx',
      'androidx.navigation:navigation-ui-ktx',
      'com.google.android.material:material'
    ])

    and: 'unused dependencies reports for lib are correct'
    def libUnused = androidProject.adviceFor('lib')
      .findAll { it.isRemove() }
      .collect { it.dependency.identifier }
    assertThat(libUnused).containsExactlyElementsIn([
      'androidx.constraintlayout:constraintlayout',
      'androidx.appcompat:appcompat',
      'androidx.core:core-ktx',
      'androidx.navigation:navigation-fragment-ktx',
      'androidx.navigation:navigation-ui-ktx',
      'com.google.android.material:material'
    ])

    and: 'abi reports are correct'
    def actualAbi = androidProject.abiReportFor('lib')
    def expectedAbi = ['androidx.core:core', 'org.jetbrains.kotlin:kotlin-stdlib']
    assertThat(actualAbi).containsExactlyElementsIn(expectedAbi)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
