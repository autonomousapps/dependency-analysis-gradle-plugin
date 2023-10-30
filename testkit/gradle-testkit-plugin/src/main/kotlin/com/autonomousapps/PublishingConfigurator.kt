package com.autonomousapps

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal class PublishingConfigurator(private val project: Project) {

  private val repoName = "FunctionalTests"

  val funcTestRepoName = "functionalTestRepo"
  val funcTestRepo: File = File(project.rootDir, "build/$funcTestRepoName").absoluteFile

  val installForFunctionalTest: TaskProvider<Task> = project.tasks.register("installForFunctionalTest") {
    // install this project's publications
    it.dependsOn("publishAllPublicationsTo${repoName}Repository")
  }

  init {
    configure()
  }

  private fun configure(): Unit = project.run {
    pluginManager.apply("maven-publish")

    extensions.getByType(PublishingExtension::class.java).repositories { h ->
      h.maven { r ->
        r.name = repoName
        r.url = uri(funcTestRepo)
      }
    }

    afterEvaluate {
      // Install dependency projects
      val installationTasks = configurations.getAt("runtimeClasspath").allDependencies
        .filterIsInstance<ProjectDependency>()
        .map { "${it.dependencyProject.path}:installForFunctionalTest" }

      installForFunctionalTest.configure {
        // all dependency projects
        it.dependsOn(installationTasks)
      }
    }
  }
}

