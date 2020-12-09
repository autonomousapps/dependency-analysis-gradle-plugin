package com.autonomousapps.kit

class Plugin(
  val id: String,
  val version: String? = null,
  val apply: Boolean = true
) {

  companion object {
    const val KOTLIN_VERSION = "1.3.72"

    val dependencyAnalysisPlugin = Plugin(
      "com.autonomousapps.dependency-analysis",
      System.getProperty("com.autonomousapps.pluginversion")
    )

    @JvmStatic val kotlinAndroidPlugin = Plugin("kotlin-android")
    @JvmStatic val javaLibraryPlugin = Plugin("java-library")
    @JvmStatic val javaPlugin = Plugin("java")
    @JvmStatic val applicationPlugin = Plugin("application")

    @JvmStatic val springBootPlugin = Plugin("org.springframework.boot", "2.3.1.RELEASE")

    @JvmStatic val androidAppPlugin = Plugin("com.android.application")
    @JvmStatic val androidLibPlugin = Plugin("com.android.library")

    @JvmStatic val kotlinPluginNoVersion = Plugin("org.jetbrains.kotlin.jvm", null, true)
    @JvmStatic val kaptPlugin = Plugin("org.jetbrains.kotlin.kapt")

    @JvmStatic val antlrPlugin = Plugin("antlr")

    @JvmStatic
    fun kotlinPlugin(version: String? = KOTLIN_VERSION, apply: Boolean = true): Plugin {
      return Plugin("org.jetbrains.kotlin.jvm", version, apply)
    }
  }

  override fun toString(): String {
    var s = "id '$id'"
    if (version != null) {
      s += " version '$version'"
    }
    if (!apply) {
      s += " apply false"
    }
    return s
  }
}
