package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependencies
import com.autonomousapps.kit.gradle.Dependency.Companion.classpath
import com.autonomousapps.kit.gradle.Dependency.Companion.project
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repositories
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PluginTest {

  @Test fun `can apply plugin to multi-project`() {
    // Given
    val project = PluginProject()
    val gradleProject = project.gradleProject

    // When we run the functional tests (dry run)
    val result = build(gradleProject.rootDir, ":plugin:functionalTest", "-m")

    // Then all the installation tasks are in the output
    assertThat(result.output.split('\n')).containsAtLeast(
      ":lib:publishAllPublicationsToFunctionalTestRepository SKIPPED",
      ":lib:installForFunctionalTest SKIPPED",
      ":plugin:publishAllPublicationsToFunctionalTestRepository SKIPPED",
      ":plugin:installForFunctionalTest SKIPPED",
      ":plugin:functionalTest SKIPPED"
    ).inOrder()
  }
}

private class PluginProject : AbstractGradleProject() {

  val gradleProject: GradleProject = build()

  private fun build(): GradleProject {
    return newGradleProjectBuilder()
      .withRootProject {
        withBuildScript {
          buildscript = BuildscriptBlock(
            repositories = Repositories.DEFAULT_PLUGINS,
            dependencies = Dependencies(classpath("com.autonomousapps:gradle-testkit-plugin:+"))
          )
        }
      }
      .withSubproject("plugin") {
        withBuildScript {
          plugins(Plugin.javaGradle, Plugin("com.autonomousapps.testkit"))
          dependencies(project("implementation", ":lib"))
        }
      }
      .withSubproject("lib") {
        withBuildScript {
          plugins(Plugin.javaLibrary, Plugin("com.autonomousapps.testkit"))
        }
      }
      .write()
  }
}
