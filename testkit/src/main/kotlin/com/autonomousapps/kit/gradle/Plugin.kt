package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Plugin @JvmOverloads constructor(
  public val id: String,
  public val version: String? = null,
  public val apply: Boolean = true,
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

  public companion object {
    public const val KOTLIN_VERSION: String = "1.9.0"

    @JvmOverloads
    @JvmStatic
    public fun of(
      id: String,
      version: String? = null,
      apply: Boolean = true,
    ): Plugin = Plugin(id, version, apply)

    @JvmStatic public val dagpId: String = "com.autonomousapps.dependency-analysis"
    @JvmStatic public val dependencyAnalysisPlugin: Plugin =
      Plugin(dagpId, System.getProperty("com.autonomousapps.pluginversion"))

    @JvmStatic public val antlrPlugin: Plugin = Plugin("antlr")
    @JvmStatic public val applicationPlugin: Plugin = Plugin("application")
    @JvmStatic public val androidAppPlugin: Plugin = Plugin("com.android.application")
    @JvmStatic public val androidLibPlugin: Plugin = Plugin("com.android.library")
    @JvmStatic public val gradleEnterprisePlugin: Plugin = Plugin("com.gradle.enterprise", "3.11.4")
    @JvmStatic public val groovyPlugin: Plugin = Plugin("groovy")
    @JvmStatic public val groovyGradlePlugin: Plugin = Plugin("groovy-gradle-plugin")
    @JvmStatic public val javaPlugin: Plugin = Plugin("java")
    @JvmStatic public val javaGradlePlugin: Plugin = Plugin("java-gradle-plugin")
    @JvmStatic public val javaLibraryPlugin: Plugin = Plugin("java-library")
    @JvmStatic public val javaTestFixturesPlugin: Plugin = Plugin("java-test-fixtures")
    @JvmStatic public val kotlinAndroidPlugin: Plugin = Plugin("org.jetbrains.kotlin.android")
    @JvmStatic public val kotlinPluginNoVersion: Plugin = Plugin("org.jetbrains.kotlin.jvm", null, true)
    @JvmStatic public val kaptPlugin: Plugin = Plugin("org.jetbrains.kotlin.kapt")
    @JvmStatic public val springBootPlugin: Plugin = Plugin("org.springframework.boot", "2.7.14")
    @JvmStatic public val scalaPlugin: Plugin = Plugin("scala")
    @JvmStatic public val warPlugin: Plugin = Plugin("war")

    @JvmStatic
    public fun kotlinPlugin(version: String? = KOTLIN_VERSION, apply: Boolean = true): Plugin {
      return Plugin("org.jetbrains.kotlin.jvm", version, apply)
    }
  }
}
