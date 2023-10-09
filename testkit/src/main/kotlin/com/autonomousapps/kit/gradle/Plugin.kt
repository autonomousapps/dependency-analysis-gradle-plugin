package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Plugin @JvmOverloads constructor(
  val id: String,
  val version: String? = null,
  val apply: Boolean = true,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("id '")
    s.append(id)
    s.append("'")
    version?.let { v ->
      s.append(" version '")
      s.append(v)
      s.append("'")
    }
    if (!apply) {
      s.append(" apply false")
    }
  }

  companion object {
    const val KOTLIN_VERSION = "1.9.0"

    @JvmOverloads
    @JvmStatic
    fun of(
      id: String,
      version: String? = null,
      apply: Boolean = true,
    ): Plugin = Plugin(id, version, apply)

    @JvmStatic val dagpId = "com.autonomousapps.dependency-analysis"
    @JvmStatic val dependencyAnalysisPlugin = Plugin(dagpId, System.getProperty("com.autonomousapps.pluginversion"))

    @JvmStatic val antlrPlugin = Plugin("antlr")
    @JvmStatic val applicationPlugin = Plugin("application")
    @JvmStatic val androidAppPlugin = Plugin("com.android.application")
    @JvmStatic val androidLibPlugin = Plugin("com.android.library")
    @JvmStatic val gradleEnterprisePlugin = Plugin("com.gradle.enterprise", "3.11.4")
    @JvmStatic val groovyPlugin = Plugin("groovy")
    @JvmStatic val groovyGradlePlugin = Plugin("groovy-gradle-plugin")
    @JvmStatic val javaPlugin = Plugin("java")
    @JvmStatic val javaGradlePlugin = Plugin("java-gradle-plugin")
    @JvmStatic val javaLibraryPlugin = Plugin("java-library")
    @JvmStatic val javaTestFixturesPlugin = Plugin("java-test-fixtures")
    @JvmStatic val kotlinAndroidPlugin = Plugin("org.jetbrains.kotlin.android")
    @JvmStatic val kotlinPluginNoVersion = Plugin("org.jetbrains.kotlin.jvm", null, true)
    @JvmStatic val kaptPlugin = Plugin("org.jetbrains.kotlin.kapt")
    @JvmStatic val springBootPlugin = Plugin("org.springframework.boot", "2.7.14")
    @JvmStatic val scalaPlugin = Plugin("scala")
    @JvmStatic val warPlugin = Plugin("war")

    @JvmStatic
    fun kotlinPlugin(version: String? = KOTLIN_VERSION, apply: Boolean = true): Plugin {
      return Plugin("org.jetbrains.kotlin.jvm", version, apply)
    }
  }
}
