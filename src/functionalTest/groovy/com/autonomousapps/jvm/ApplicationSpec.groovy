package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ApplicationProject
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ApplicationSpec extends AbstractJvmSpec {

  def "can analyze kotlin-jvm application projects when kotlin-jvm is applied first (#gradleVersion)"() {
    given:
    def plugins = [Plugins.kotlinNoVersion, Plugin.application]
    def project = new ApplicationProject(plugins, SourceType.KOTLIN)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze kotlin-jvm application projects when application is applied first (#gradleVersion)"() {
    given:
    def plugins = [Plugin.application, Plugins.kotlinNoVersion]
    def project = new ApplicationProject(plugins, SourceType.KOTLIN)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
