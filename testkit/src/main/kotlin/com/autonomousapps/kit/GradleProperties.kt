package com.autonomousapps.kit

class GradleProperties(
  private val lines: List<String>
) {

  companion object {
    val JVM_ARGS = """
      # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
      org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:GCTimeLimit=20 -XX:GCHeapFreeLimit=10 -XX:MaxMetaspaceSize=512m      
    """.trimIndent()

    val USE_ANDROID_X = """
      # Necessary for AGP 3.6+
      android.useAndroidX=true
    """.trimIndent()

    @JvmStatic
    fun of(vararg lines: String): GradleProperties = GradleProperties(lines.toList())

    @JvmStatic
    fun minimalJvmProperties(): GradleProperties = of(JVM_ARGS)

    @JvmStatic
    fun minimalAndroidProperties(): GradleProperties = of(JVM_ARGS, USE_ANDROID_X)
  }

  override fun toString(): String =
    if (lines.isEmpty()) {
      ""
    } else {
      lines.joinToString("\n")
    }
}