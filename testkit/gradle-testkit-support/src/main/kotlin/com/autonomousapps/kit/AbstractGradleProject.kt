package com.autonomousapps.kit

import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories

/**
 * Provides some common functionality for Gradle functional tests.
 */
public abstract class AbstractGradleProject @JvmOverloads constructor(
  buildPath: String = "build/functionalTest",
) {

  public companion object {
    /**
     * Should be the version of your plugin-under-test. Might be an empty string if:
     * 1. You are using this library without also using the plugin `com.autonomousapps.testkit`, or
     * 2. You have your plugin version set to an empty string.
     *
     * Never null.
     */
    @JvmField
    public val PLUGIN_UNDER_TEST_VERSION: String = System.getProperty(
      "com.autonomousapps.plugin-under-test.version", ""
    )

    /**
     * The absolute path to the filesystem location for the repository for your plugin-under-test and its project
     * dependencies. Might be an empty string if:
     * 1. You are using this library without also using the plugin `com.autonomousapps.testkit`
     *
     * Never null.
     */
    @JvmField
    public val FUNC_TEST_REPO: String = System.getProperty("com.autonomousapps.plugin-under-test.repo", "")

    /**
     * The absolute path to the filesystem location for the repository(ies) for any included builds your
     * plugin-under-test relies on. Might be empty (and might contain empty elements). To ensure correct configuration,
     * use:
     *
     * ```
     * // plugin-under-test/build.gradle
     * plugins {
     *   id 'com.autonomousapps.testkit'
     * }
     *
     * gradleTestKitSupport {
     *   includeProjects(
     *     "included-build-1:fully:qualified:project:path",
     *     "included-build-2:fully:qualified:project:path",
     *     ...
     *   )
     * }
     * ```
     * and
     * ```
     * // included-build-1/<fully/qualified/project/path>/build.gradle
     * plugins {
     *   id 'com.autonomousapps.testkit'
     * }
     * ```
     *
     * Never null.
     */
    @JvmField
    public val FUNC_TEST_INCLUDED_BUILD_REPOS: List<String> =
      System.getProperty("com.autonomousapps.plugin-under-test.repos-included", "").split(',')
  }

  protected open fun newGradleProjectBuilder(dslKind: GradleProject.DslKind = GradleProject.DslKind.GROOVY): GradleProject.Builder {
    return GradleProject.Builder(rootDir.toFile(), dslKind)
  }

  /**
   * The root directory of a Gradle build. The default value is
   * ```
   * <PWD>/build/functionalTest/<ConcreteClassSimpleName>-<UUID>[-Gradle worker ID (if present)]
   * ```
   */
  public val rootDir: Path = File("$buildPath/${newSlug()}").toPath().createDirectories()

  private fun newSlug(): String {
    var worker = System.getProperty("org.gradle.test.worker", "")
    if (worker.isNotEmpty()) {
      worker = "-$worker"
    }
    return "${javaClass.simpleName}-${UUID.randomUUID().toString().take(8)}$worker"
  }
}
