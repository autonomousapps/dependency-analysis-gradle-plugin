package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.AbstractGradleProject.Companion.FUNC_TEST_INCLUDED_BUILD_REPOS
import com.autonomousapps.kit.AbstractGradleProject.Companion.FUNC_TEST_REPO
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public sealed class Repository : Element.Line {

  public data class Method(private val repo: String) : Repository() {

    override fun render(scribe: Scribe): String = scribe.line { s ->
      s.append(repo)
    }
  }

  public data class Url(private val repo: String) : Repository() {

    override fun render(scribe: Scribe): String = scribe.line { s ->
      s.append(repo)
    }
  }

  public companion object {
    @JvmField public val GOOGLE: Repository = Method("google()")
    @JvmField public val GRADLE_PLUGIN_PORTAL: Repository = Method("gradlePluginPortal()")
    @JvmField public val LIBS: Repository = Url("flatDir { 'libs' }")
    @JvmField public val MAVEN_CENTRAL: Repository = Method("mavenCentral()")
    @JvmField public val MAVEN_LOCAL: Repository = Method("mavenLocal()")
    @JvmField public val SNAPSHOTS: Repository = ofMaven("https://oss.sonatype.org/content/repositories/snapshots/")

    /**
     * The repository for local projects if you're using the plugin `com.autonomousapps.testkit`. If not, a broken,
     * unusable repo.
     */
    @JvmField public val FUNC_TEST: Repository = ofMaven(FUNC_TEST_REPO)

    /**
     * The repository for local projects if you're using the plugin `com.autonomousapps.testkit` in your included
     * builds. If not, an empty list. See documentation on that plugin for how to configure.
     */
    @JvmField public val FUNC_TEST_INCLUDED_BUILDS: List<Repository> = FUNC_TEST_INCLUDED_BUILD_REPOS
      .filterNot { it.isEmpty() }
      .map { ofMaven(it) }

    @JvmField
    public val DEFAULT: List<Repository> = listOf(
      FUNC_TEST,
      GOOGLE,
      MAVEN_CENTRAL,
    )

    @JvmField
    public val DEFAULT_PLUGINS: List<Repository> = listOf(
      GRADLE_PLUGIN_PORTAL,
      MAVEN_CENTRAL,
    )

    @JvmStatic
    public fun ofMaven(repoUrl: String): Repository {
      return Url("maven { url = \"$repoUrl\" }")
    }
  }
}
