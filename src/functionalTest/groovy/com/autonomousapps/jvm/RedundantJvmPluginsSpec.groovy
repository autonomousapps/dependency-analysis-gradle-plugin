package com.autonomousapps.jvm

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.jvm.projects.RedundantJvmPluginsProject

import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class RedundantJvmPluginsSpec extends AbstractJvmSpec {

  def "kotlin-jvm plugin is redundant (#gradleVersion)"() {
    given:
    def project = new RedundantJvmPluginsProject()
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    def buildHealth = project.actualBuildHealth()
    def projAdvice = buildHealth.find { it.projectPath == ':proj' }
    assertThat(buildHealth).containsExactlyElementsIn(project.expectedBuildHealth)
    assertThat(projAdvice.pluginAdvice).contains(PluginAdvice.redundantKotlinJvm())

    where:
    gradleVersion << gradleVersions()
  }
}
