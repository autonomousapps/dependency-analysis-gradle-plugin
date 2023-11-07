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
import com.autonomousapps.kit.truth.BuildTaskListSubject.Companion.buildTaskList
import com.autonomousapps.kit.truth.BuildTaskSubject.Companion.buildTasks
import com.google.common.truth.Truth.assertAbout
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

internal class PluginTest {

  @Test fun `can apply plugin to a multi-project and composite build`() {
    // Given
    val project = PluginProject()
    val gradleProject = project.gradleProject

    // When we run the functional tests
    val result = build(gradleProject.rootDir, ":plugin:functionalTest")

    // Then the installation tasks ran in order, followed by the test task
    assertAbout(buildTaskList()).that(result.tasks).containsAtLeastPathsIn(
      ":build-logic:lib:publishAllPublicationsToFunctionalTestRepository",
      ":build-logic:lib:installForFunctionalTest",
      ":lib:publishAllPublicationsToFunctionalTestRepository",
      ":lib:installForFunctionalTest",
      ":plugin:publishAllPublicationsToFunctionalTestRepository",
      ":plugin:installForFunctionalTest",
      ":plugin:functionalTest",
    ).inOrder()

    // and the build-logic installation tasks were successful
    val successLike = listOf(TaskOutcome.FROM_CACHE, TaskOutcome.UP_TO_DATE, TaskOutcome.SUCCESS)

    assertAbout(buildTasks())
      .that(result.task(":build-logic:lib:publishAllPublicationsToFunctionalTestRepository"))
      .hasOutcomeIn(successLike)
    assertAbout(buildTasks())
      .that(result.task(":build-logic:lib:installForFunctionalTest"))
      .hasOutcomeIn(successLike)

    // and the main build installation tasks were successful
    assertAbout(buildTasks())
      .that(result.task(":lib:publishAllPublicationsToFunctionalTestRepository"))
      .hasOutcomeIn(successLike)
    assertAbout(buildTasks())
      .that(result.task(":lib:installForFunctionalTest"))
      .hasOutcomeIn(successLike)
    assertAbout(buildTasks())
      .that(result.task(":plugin:publishAllPublicationsToFunctionalTestRepository"))
      .hasOutcomeIn(successLike)
    assertAbout(buildTasks())
      .that(result.task(":plugin:installForFunctionalTest"))
      .hasOutcomeIn(successLike)

    // and the test task didn't run because it had no source
    assertAbout(buildTasks())
      .that(result.task(":plugin:functionalTest"))
      .noSource()
  }
}

private class PluginProject : AbstractGradleProject() {

  val gradleProject: GradleProject = build()

  private fun build(): GradleProject {
    val buildscriptBlock = BuildscriptBlock(
      repositories = Repositories.DEFAULT_PLUGINS,
      dependencies = Dependencies(classpath("com.autonomousapps:gradle-testkit-plugin:+"))
    )
    val testkitPlugin = Plugin("com.autonomousapps.testkit")

    return newGradleProjectBuilder()
      // included build
      .withIncludedBuild("build-logic") {
        withRootProject {
          withBuildScript {
            buildscript = buildscriptBlock
          }
        }
        withSubproject("lib") {
          withBuildScript {
            plugins(Plugin.javaLibrary, testkitPlugin)
          }
        }
      }
      // main build
      .withRootProject {
        settingsScript.apply {
          additions = """
            includeBuild 'build-logic'
          """.trimIndent()
        }
        withBuildScript {
          buildscript = buildscriptBlock
        }
      }
      .withSubproject("plugin") {
        withBuildScript {
          plugins(Plugin.javaGradle, testkitPlugin)
          dependencies(project("implementation", ":lib"))
          withGroovy(
            """
              gradleTestKitSupport {
                withIncludedBuildProjects('build-logic:lib')
              }
            """.trimIndent()
          )
        }
      }
      .withSubproject("lib") {
        withBuildScript {
          plugins(Plugin.javaLibrary, testkitPlugin)
        }
      }
      .write()
  }
}
