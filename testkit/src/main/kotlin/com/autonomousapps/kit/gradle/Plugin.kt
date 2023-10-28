package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Plugin @JvmOverloads constructor(
  public val id: String,
  public val version: String? = null,
  public val apply: Boolean = true,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("id ")
    s.quoted(id)
    version?.let { v ->
      s.append(" version ")
      s.quoted(v)
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
    @JvmStatic public val dependencyAnalysis: Plugin = Plugin(dagpId, System.getProperty("com.autonomousapps.pluginversion"))
    @JvmStatic public val antlr: Plugin = Plugin("antlr")
    @JvmStatic public val application: Plugin = Plugin("application")
    @JvmStatic public val androidApp: Plugin = Plugin("com.android.application")
    @JvmStatic public val androidLib: Plugin = Plugin("com.android.library")
    @JvmStatic public val gradleEnterprise: Plugin = Plugin("com.gradle.enterprise", "3.11.4")
    @JvmStatic public val groovy: Plugin = Plugin("groovy")
    @JvmStatic public val groovyGradle: Plugin = Plugin("groovy-gradle-plugin")
    @JvmStatic public val java: Plugin = Plugin("java")
    @JvmStatic public val javaGradle: Plugin = Plugin("java-gradle-plugin")
    @JvmStatic public val javaLibrary: Plugin = Plugin("java-library")
    @JvmStatic public val javaTestFixtures: Plugin = Plugin("java-test-fixtures")
    @JvmStatic public val kotlinAndroid: Plugin = Plugin("org.jetbrains.kotlin.android")
    @JvmStatic public val kotlinNoVersion: Plugin = Plugin("org.jetbrains.kotlin.jvm", null, true)
    @JvmStatic public val kapt: Plugin = Plugin("org.jetbrains.kotlin.kapt")
    @JvmStatic public val springBoot: Plugin = Plugin("org.springframework.boot", "2.7.14")
    @JvmStatic public val scala: Plugin = Plugin("scala")
    @JvmStatic public val war: Plugin = Plugin("war")

    @JvmStatic
    public fun kotlinPlugin(version: String? = KOTLIN_VERSION, apply: Boolean = true): Plugin {
      return Plugin("org.jetbrains.kotlin.jvm", version, apply)
    }
  }
}
