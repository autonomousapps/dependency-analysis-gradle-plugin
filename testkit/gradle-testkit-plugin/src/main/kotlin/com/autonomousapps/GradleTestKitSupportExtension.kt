package com.autonomousapps

import org.gradle.api.Project

public abstract class GradleTestKitSupportExtension(private val project: Project) {

  /**
   * Adds a dependency on `com.autonomousapps:gradle-testkit-support` with version [DEFAULT_SUPPORT_VERSION] unless
   * otherwise specified.
   */
  @JvmOverloads
  public fun withSupportLibrary(version: String = DEFAULT_SUPPORT_VERSION) {
    addDependency(
      configuration = "functionalTestImplementation",
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
      configuration = "functionalTestImplementation",
      dependency = "com.autonomousapps:gradle-testkit-truth:$version"
    )
  }

  private fun addDependency(configuration: String, dependency: String) {
    project.dependencies.run {
      add(
        configuration,
        dependency
      )
    }
  }

  internal companion object {

    private const val DEFAULT_SUPPORT_VERSION = "0.7"
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
