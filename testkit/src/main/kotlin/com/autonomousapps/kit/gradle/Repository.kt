package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

sealed class Repository : Element.Line {

  class Method(private val repo: String) : Repository() {

    override fun render(scribe: Scribe): String = scribe.line { s ->
      s.append(repo)
    }
  }

  class Url(private val repo: String) : Repository() {

    override fun render(scribe: Scribe): String = scribe.line { s ->
      s.append(repo)
    }
  }

  companion object {
    @JvmField val GOOGLE: Repository = Method("google()")
    @JvmField val GRADLE_PLUGIN_PORTAL: Repository = Method("gradlePluginPortal()")
    @JvmField val LIBS: Repository = Url("flatDir { 'libs' }")
    @JvmField val MAVEN_CENTRAL: Repository = Method("mavenCentral()")
    @JvmField val MAVEN_LOCAL: Repository = Method("mavenLocal()")
    @JvmField val SNAPSHOTS: Repository = ofMaven("https://oss.sonatype.org/content/repositories/snapshots/")

    // Kotlin DSL example
    //repositories {
    //  maven(url = "https://...")
    //}

    @JvmField
    val DEFAULT = listOf(
      GOOGLE,
      MAVEN_CENTRAL,
    )

    @JvmField
    val DEFAULT_PLUGINS = listOf(
      GRADLE_PLUGIN_PORTAL,
      MAVEN_CENTRAL,
    )

    @JvmStatic
    fun ofMaven(repoUrl: String): Repository {
      return Url("maven { url = \"$repoUrl\" }")
    }
  }
}
