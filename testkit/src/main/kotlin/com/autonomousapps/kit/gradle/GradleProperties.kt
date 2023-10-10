package com.autonomousapps.kit.gradle

public class GradleProperties(
  private val lines: List<String>,
) {

  public operator fun plus(other: GradleProperties): GradleProperties {
    return GradleProperties(
      (lines + other.lines).distinct()
    )
  }

  public operator fun plus(other: String): GradleProperties {
    return GradleProperties(
      (lines + other).distinct()
    )
  }

  public operator fun plus(other: List<String>): GradleProperties {
    return GradleProperties(
      (lines + other).distinct()
    )
  }

  @Suppress("MemberVisibilityCanBePrivate")
  public companion object {
    public val JVM_ARGS: String = """
      # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
      org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:GCTimeLimit=20 -XX:GCHeapFreeLimit=10 -XX:MaxMetaspaceSize=1024m      
    """.trimIndent()

    public val USE_ANDROID_X: String = """
      # Necessary for AGP 3.6+
      android.useAndroidX=true
    """.trimIndent()

    public const val NON_TRANSITIVE_R: String = "android.nonTransitiveRClass=true"

    @JvmStatic
    public fun of(vararg lines: String): GradleProperties = GradleProperties(lines.toList())

    @JvmStatic
    public fun minimalJvmProperties(): GradleProperties = of(JVM_ARGS)

    @JvmStatic
    public fun minimalAndroidProperties(): GradleProperties = of(JVM_ARGS, USE_ANDROID_X, NON_TRANSITIVE_R)
  }

  override fun toString(): String {
    return if (lines.isEmpty()) {
      ""
    } else {
      lines.joinToString("\n")
    }
  }
}
