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

    val kotlinAndroidPlugin = Plugin("kotlin-android")
    val javaLibraryPlugin = Plugin("java-library")
    val javaPlugin = Plugin("java")
    val applicationPlugin = Plugin("application")

    val springBootPlugin = Plugin("org.springframework.boot", "2.3.1.RELEASE")

    val androidAppPlugin = Plugin("com.android.application")
    val androidLibPlugin = Plugin("com.android.library")

    val kotlinPluginNoVersion = Plugin("org.jetbrains.kotlin.jvm", null, true)
    val kaptPlugin = Plugin("org.jetbrains.kotlin.kapt")

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
