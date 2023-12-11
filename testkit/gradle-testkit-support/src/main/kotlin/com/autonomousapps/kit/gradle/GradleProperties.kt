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

  public companion object {
    public val JVM_ARGS: String = """
      # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
      org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:MaxMetaspaceSize=1024m
    """.trimIndent()

    public val USE_ANDROID_X: String = """
      # Necessary for AGP 3.6+
      android.useAndroidX=true
    """.trimIndent()

    public const val NON_TRANSITIVE_R: String = "android.nonTransitiveRClass=true"

    /** Enable the configuration cache, pre-Gradle 8. */
    public const val CONFIGURATION_CACHE_UNSTABLE: String = "org.gradle.unsafe.configuration-cache=true"

    /** Enable the configuration cache, Gradle 8+. */
    public const val CONFIGURATION_CACHE_STABLE: String = "org.gradle.configuration-cache=true"

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
    public fun enableConfigurationCache(): GradleProperties = of(CONFIGURATION_CACHE_STABLE, CONFIGURATION_CACHE_UNSTABLE)
  }

  override fun toString(): String {
    return if (lines.isEmpty()) {
      ""
    } else {
      lines.joinToString("\n")
    }
  }
}
