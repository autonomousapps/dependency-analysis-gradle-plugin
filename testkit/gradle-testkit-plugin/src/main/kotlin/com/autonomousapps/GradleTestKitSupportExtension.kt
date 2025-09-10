// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.GradleTestKitSupportExtension.Companion.DEFAULT_SUPPORT_VERSION
import com.autonomousapps.GradleTestKitSupportExtension.Companion.DEFAULT_TRUTH_VERSION
import com.autonomousapps.internal.Configurer
import com.autonomousapps.internal.arguments.IncludeBuildReposArgumentProvider
import com.autonomousapps.internal.capitalizeSafely
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import java.io.File

/**
 * Usage:
 *
 * ```
 * gradleTestKitSupport {
 *   // Plugin projects get this automatically. Other projects may use the Gradle API but not be projects. For those
 *   // projects, one may wish to run functional tests as well.
 *   registerFunctionalTest()
 *
 *   withIncludedBuildProjects("build-logic:plugin", ...)
 *   withClasspaths("myCustomClasspath", ...)
 *   disablePublication()
 * }
 * ```
 */
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
  internal val installForFunctionalTest: TaskProvider<Task> = project.tasks.register(taskName) { t ->
    t.group = "publishing"
    t.description = "Publishes all publications to the $repoName repository."
    // install this project's publications
    t.dependsOn("publishAllPublicationsTo${repoName}Repository")
  }

  private val projects: ListProperty<String> = project.objects.listProperty(String::class.java)
  private val includedBuildRepos = mutableListOf<String>()
  private var testTask: TaskProvider<Test>? = null
  private val disablePub: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

  private val configurer: Configurer = Configurer(project, this)

  private lateinit var publishing: PublishingExtension

  init {
    configure()
  }

  /**
   * Plugin projects get this automatically. Other projects may use the Gradle API but not be projects. For those
   * projects, one may wish to run functional tests as well.
   */
  public fun registerFunctionalTest() {
    configurer.configure()
  }

  /**
   * Include projects from included builds. Must be the fully-qualified path to the project. For example, if your
   * included build is named "build-logic" (you have the statement `includeBuild("build-logic")` in your settings
   * script), and the build-logic project has a subproject "plugin", then call this method like this:
   * ```
   * gradleTestKitSupport {
   *   includeProjects("build-logic:plugin", ...)
   * }
   * ```
   *
   * TODO(tsr): this can be automated.
   */
  public fun withIncludedBuildProjects(vararg projects: String) {
    if (projects.isEmpty()) {
      project.logger.warn("'projects' is empty! Nothing to install.")
      return
    }

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

  /**
   * ```
   * gradleTestKitSupport {
   *   // Install projects on `myCustomClasspath`
   *   withClasspaths("myCustomClasspath", ...)
   * }
   * ```
   */
  public fun withClasspaths(vararg classpaths: String) {
    if (classpaths.isEmpty()) {
      project.logger.warn("'classpaths' is empty! Nothing to install.")
      return
    }

    installForFunctionalTest.configure { t ->
      classpaths.forEach { classpath ->
        project.installationTasksFor(classpath)?.let { installationTasks ->
          t.dependsOn(installationTasks)
        }
      }
    }
  }

  /**
   * Disable the creation of a test publication (probably as a workaround for issues with other plugins that also
   * configure publications a little too automatically).
   */
  public fun disablePublication() {
    disablePub.set(true)
  }

  internal fun setTestTask(testTask: TaskProvider<Test>) {
    this.testTask = testTask
    configureTestTask()
  }

  private fun configureTestTask() {
    if (includedBuildRepos.isNotEmpty()) {
      testTask?.configure { t ->
        // All included build projects get installed to their own local repo
        t.jvmArgumentProviders += IncludeBuildReposArgumentProvider(includedBuildRepos)
      }
    }
  }

  private fun configure(): Unit = project.run {
    pluginManager.apply("maven-publish")
    publishing = extensions.getByType(PublishingExtension::class.java)

    // Always create our test repo
    publishing.repositories { h ->
      h.maven { r ->
        r.name = repoName
        r.url = uri(funcTestRepo)
      }
    }

    afterEvaluate {
      // Optionally create a test publication -- only if one doesn't already exist at these coordinates
      if (shouldCreateTestPublication()) {
        // Known software components in the ecosystem
        listOf("java", "javaPlatform")
          .mapNotNull { components.findByName(it) }
          .forEach { component ->
            publishing.publications { p ->
              val pubName = "testKitSupportFor${component.name.capitalizeSafely()}"
              p.create(pubName, MavenPublication::class.java) { pub ->
                pub.from(component)
              }
            }
          }
      }

      // Install all dependency projects. Must be in afterEvaluate because we need to capture dependencies, which are
      // added after the plugin is applied.
      withClasspaths("runtimeClasspath", "${sourceSetName}RuntimeClasspath")
    }
  }

  private fun Project.shouldCreateTestPublication(): Boolean {
    if (disablePub.get()) return false

    val defaultGav = Gav.of(this)
    return publishing.publications.asSequence()
      .filterIsInstance<MavenPublication>()
      .map { Gav.of(it) }
      .none { it == defaultGav }
  }

  private fun Project.installationTasksFor(classpath: String): List<String>? {
    return configurations.findByName(classpath)?.allDependencies
      ?.filterIsInstance<ProjectDependency>()
      // filter out self-dependency
      ?.filterNot { it.path == project.path }
      ?.map { "${it.path}:$taskName" }
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
    project.dependencies.add(configuration, dependency)
  }

  internal companion object {

    // TODO(tsr): can we inject these values so they're not hardcoded?
    private const val DEFAULT_SUPPORT_VERSION = "0.20"
    private const val DEFAULT_TRUTH_VERSION = "1.6.1"

    fun create(project: Project): GradleTestKitSupportExtension {
      return project.extensions.create(
        "gradleTestKitSupport",
        GradleTestKitSupportExtension::class.java,
        project
      )
    }
  }
}

private data class Gav(
  val groupId: String,
  val artifactId: String,
  val version: String,
) {
  companion object {
    /**
     * This is the group:artifact:version coordinate that Gradle will default to if none are specified when configuring
     * a new publication. We will check for an existing publication that matches these coordinates and, if found, not
     * create our own.
     */
    fun of(project: Project) = Gav(
      groupId = project.group.toString(),
      artifactId = project.name,
      version = project.version.toString()
    )

    fun of(publication: MavenPublication) = Gav(
      groupId = publication.groupId,
      artifactId = publication.artifactId,
      version = publication.version
    )
  }
}
