package com.autonomousapps.kit.gradle

public class GradleProperties(private val lines: MutableList<CharSequence>) {

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

  public operator fun plusAssign(other: CharSequence) {
    lines.add(other)
  }

  public operator fun plusAssign(other: Iterable<CharSequence>) {
    lines.addAll(other)
  }

  public operator fun plusAssign(other: GradleProperties) {
    lines.addAll(other.lines)
  }

  private fun <T> Iterable<T>.mutDistinct(): MutableList<T> {
    return toMutableSet().toMutableList()
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

    @JvmStatic
    public fun of(vararg lines: CharSequence): GradleProperties = GradleProperties(lines.toMutableList())

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
