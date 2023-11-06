package com.autonomousapps

import com.autonomousapps.internal.capitalizeSafely
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import java.io.File

public abstract class GradleTestKitSupportExtension(private val project: Project) {

  // Currently not configurable
  internal val sourceSetName = "functionalTest"

  // FunctionalTest
  private val repoName = sourceSetName.capitalizeSafely()

  // installForFunctionalTest
  private val taskName = "installFor${repoName}"

  // functionalTestRepo
  internal val funcTestRepoName = "${sourceSetName}Repo"

  // build/functionalTestRepo
  private val repoDir = "build/$funcTestRepoName"

  // $rootDir/build/functionalTestRepo
  internal val funcTestRepo: File = File(project.rootDir, repoDir).absoluteFile

  // in other words, this task is an alias for the task dependency below
  internal val installForFunctionalTest: TaskProvider<Task> = project.tasks.register(taskName) {
    // install this project's publications
    it.dependsOn("publishAllPublicationsTo${repoName}Repository")
  }

  private val projects: ListProperty<String> = project.objects.listProperty(String::class.java)
  private val includedBuildRepos = mutableListOf<String>()
  private var testTask: TaskProvider<Test>? = null

  init {
    configure()
  }

  /**
   * Include projects from included builds. Must be the fully-qualified path to the project. For example, if your
   * included build is named "build-logic" (you have the statement `includeBuild("build-logic")` in your settings
   * script), and the build-logic project has a subproject "plugin", then call this method like this:
   * ```
   * includeProjects("build-logic:plugin", ...)
   * ```
   */
  public fun includeProjects(vararg projects: String) {
    this.projects.set(projects.toList())
    this.projects.disallowChanges()

    projects.map { includedBuildProject ->
      // normalize
      val path = includedBuildProject.removePrefix(":")

      // The first part is the name of the included build, and the rest is the fully-qualified name to the project
      val index = path.indexOf(':')
      if (index == -1) {
        error(
          "Expected path to included build project (form: <included build name>:<project path>). Got '$includedBuildProject'."
        )
      }

      val includedBuild = project.gradle.includedBuild(path.substring(0, index))
      val projectPath = path.substring(index)

      // Add to list of repos for use in tests
      includedBuildRepos += File(includedBuild.projectDir, repoDir).absolutePath

      includedBuild.task("$projectPath:$taskName")
    }.forEach { taskRef ->
      // Add dependency from this project's installation task onto the included build project's installation task
      installForFunctionalTest.configure {
        it.dependsOn(taskRef)
      }
    }

    configureTestTask()
  }

  internal fun setTestTask(testTask: TaskProvider<Test>) {
    this.testTask = testTask
    configureTestTask()
  }

  private fun configureTestTask() {
    if (includedBuildRepos.isNotEmpty()) {
      testTask?.configure { t ->
        // All included build projects get installed to their own local repo
        t.systemProperty(
          "com.autonomousapps.plugin-under-test.repos-included",
          includedBuildRepos.joinToString(separator = ",")
        )
      }
    }
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
        .map { "${it.dependencyProject.path}:$taskName" }

      installForFunctionalTest.configure {
        // all dependency projects
        it.dependsOn(installationTasks)
      }
    }
  }

  /**
   * Adds a dependency on `com.autonomousapps:gradle-testkit-support` with version [DEFAULT_SUPPORT_VERSION] unless
   * otherwise specified.
   */
  @JvmOverloads
  public fun withSupportLibrary(version: String = DEFAULT_SUPPORT_VERSION) {
    addDependency(
      configuration = "${sourceSetName}Implementation",
      dependency = "com.autonomousapps:gradle-testkit-support:$version"
    )
  }

  /**
   * Adds a dependency on `com.autonomousapps:gradle-testkit-truth` with version [DEFAULT_TRUTH_VERSION] unless
   * otherwise specified.
   */
  @JvmOverloads
  public fun withTruthLibrary(version: String = DEFAULT_TRUTH_VERSION) {
    addDependency(
      configuration = "${sourceSetName}Implementation",
      dependency = "com.autonomousapps:gradle-testkit-truth:$version"
    )
  }

  @Suppress("SameParameterValue")
  private fun addDependency(configuration: String, dependency: String) {
    project.dependencies.run {
      add(
        configuration,
        dependency
      )
    }
  }

  internal companion object {

    private const val DEFAULT_SUPPORT_VERSION = "0.9"
    private const val DEFAULT_TRUTH_VERSION = "1.3"

    fun create(project: Project): GradleTestKitSupportExtension {
      return project.extensions.create(
        "gradleTestKitSupport",
        GradleTestKitSupportExtension::class.java,
        project
      )
    }
  }
}
