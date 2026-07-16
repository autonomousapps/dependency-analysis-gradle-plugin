// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

public class GradleProperties(private val lines: MutableList<String>) {

  public operator fun plus(other: CharSequence): GradleProperties {
    return GradleProperties(
      (lines + other).mutDistinct()
    )
  }

  public operator fun plus(other: Iterable<CharSequence>): GradleProperties {
    return GradleProperties(
      (lines + other).mutDistinct()
    )
  }

  public operator fun plus(other: GradleProperties): GradleProperties {
    return GradleProperties(
      (lines + other.lines).mutDistinct()
    )
  }

  private fun <T> Iterable<T>.mutDistinct(): MutableList<String> {
    return toMutableSet().map { it.toString() }.toMutableList()
  }

  @Suppress("MayBeConstant") // lies. @JvmField and const are incompatible.
  public companion object {
    @JvmField
    public val JVM_ARGS: String = """
      # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
      org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:MaxMetaspaceSize=1024m
    """.trimIndent()

    @JvmField
    public val USE_ANDROID_X: String = """
      # Necessary for AGP 3.6+
      android.useAndroidX=true
    """.trimIndent()

    @JvmField
    public val NON_TRANSITIVE_R: String = "android.nonTransitiveRClass=true"

    /** Enable the build cache. */
    @JvmField
    public val BUILD_CACHE: String = "org.gradle.caching=true"

    /** Enable the configuration cache, pre-Gradle 8. */
    @JvmField
    public val CONFIGURATION_CACHE_UNSTABLE: String = "org.gradle.unsafe.configuration-cache=true"

    /** Enable the configuration cache, Gradle 8+. */
    @JvmField
    public val CONFIGURATION_CACHE_STABLE: String = "org.gradle.configuration-cache=true"

    /** Enable parallel store and load for configuration cache entries, from Gradle 8.11. */
    @JvmField
    public val CONFIGURATION_CACHE_PARALLEL: String = "org.gradle.configuration-cache.parallel=true"

    /**
     * Enable isolated projects, pre-Gradle 9.
     *
     * @see <a href="https://docs.gradle.org/nightly/userguide/isolated_projects.html">Isolated Projects</a>
     */
    @JvmField
    public val ISOLATED_PROJECTS_UNSTABLE: String = "org.gradle.unsafe.isolated-projects=true"

    /** Enable parallel builds. */
    @JvmField
    public val PARALLEL: String = "org.gradle.parallel=true"

    /**
     * Disable the behavior of the Kotlin Gradle Plugin that adds the stdlib as an `api` dependency by default.
     *
     * @see <a href="https://kotlinlang.org/docs/gradle-configure-project.html#dependency-on-the-standard-library">Dependency on the standard library</a>
     */
    @JvmField
    public val KOTLIN_STDLIB_NO_DEFAULT_DEPS: String = "kotlin.stdlib.default.dependency=false"

    @JvmStatic
    public fun of(vararg lines: CharSequence): GradleProperties {
      // normalize
      val theLines = lines.asSequence()
        .flatMap { it.split('\n') }
        .map { it.trim() }
        .toMutableList()

      return GradleProperties(theLines)
    }

    @JvmStatic
    public fun minimalJvmProperties(): GradleProperties = of(JVM_ARGS)

    @JvmStatic
    public fun minimalAndroidProperties(): GradleProperties = of(JVM_ARGS, USE_ANDROID_X, NON_TRANSITIVE_R)

    @JvmStatic
    public fun enableConfigurationCache(): GradleProperties = of(CONFIGURATION_CACHE_STABLE)

    @JvmStatic
    public fun enableIsolatedProjects(): GradleProperties = of(ISOLATED_PROJECTS_UNSTABLE)

    /**
     * Disable the behavior of the Kotlin Gradle Plugin that adds the stdlib as an `api` dependency by default.
     *
     * @see <a href="https://kotlinlang.org/docs/gradle-configure-project.html#dependency-on-the-standard-library">Dependency on the standard library</a>
     */
    @JvmStatic
    public fun kotlinStdlibNoDefaultDeps(): GradleProperties = of(KOTLIN_STDLIB_NO_DEFAULT_DEPS)
  }

  override fun toString(): String {
    return if (lines.isEmpty()) {
      ""
    } else {
      lines.joinToString("\n")
    }
  }
}
