// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.AbstractGradleProject.Companion.FUNC_TEST_INCLUDED_BUILD_REPOS
import com.autonomousapps.kit.AbstractGradleProject.Companion.FUNC_TEST_REPO
import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe
import com.autonomousapps.kit.render.escape

/**
 * ```
 * // Groovy DSL
 * maven { url 'https://repo.spring.io/release' }
 * mavenCentral()
 *
 * // Kotlin DSL
 * // 1
 * maven { url = uri("https://repo.spring.io/release") }
 * mavenCentral()
 *
 * // 2
 * maven(url = "https://repo.spring.io/release")
 * mavenCentral()
 *
 * // 3
 * exclusiveContent {
 *   forRepository {
 *     maven(url = "https://repo.spring.io/release")
 *   }
 *   filter {
 *     includeGroup("...")
 *   }
 * }
 *
 * // 4
 * maven(url = "s3://my-repo/maven") {
 *   authentication { create<AwsImAuthentication>("awsIm") }
 * }
 * ```
 */
public sealed class Repository : Repositories.Element {

  public interface Configurable {
    public fun withConfigureAction(configureAction: String): Configurable
  }

  public data class ExclusiveContent(
    val repo: Repository,
    val filters: List<String>,
  ) : Repository(), Element.Block {

    override val name: String = "exclusiveContent"

    override fun render(scribe: Scribe): String = scribe.block(this) { s ->
      s.block("forRepository") { s ->
        repo.render(s)
      }
      s.block("filter") { s ->
        filters.forEach { filter ->
          s.line { it.append(filter) }
        }
      }
    }
  }

  public data class FlatDir(val repoUrl: String) : Repository(), Element.Line {
    override fun render(scribe: Scribe): String = scribe.line { s ->
      s.append("flatDir { ")
      s.appendQuoted(repoUrl)
      s.append(" }")
    }
  }

  public data class Method @JvmOverloads constructor(
    val repoCall: String,
    val configureAction: String? = null,
  ) : Repository(), Element.Line, Configurable {
    override fun render(scribe: Scribe): String {
      return if (configureAction == null) {
        scribe.line { s ->
          s.append(repoCall)
          s.append("()")
        }
      } else {
        scribe.block(repoCall) { s ->
          s.line { it.append(configureAction) }
        }
      }
    }

    public override fun withConfigureAction(configureAction: String): Method {
      return copy(configureAction = configureAction)
    }
  }

  public data class Url @JvmOverloads constructor(
    val repoUrl: String,
    val configureAction: String? = null,
  ) : Repository(), Element.Line, Configurable {
    override fun render(scribe: Scribe): String = when (scribe.dslKind) {
      DslKind.GROOVY -> renderGroovy(scribe)
      DslKind.KOTLIN -> renderKotlin(scribe)
    }

    private fun renderGroovy(scribe: Scribe): String {
      return if (configureAction == null) {
        scribe.line { s ->
          s.append("maven { url = ")
          s.appendQuoted(repoUrl)
          s.append(" }")
        }
      } else {
        scribe.block("maven") { s ->
          s.line {
            it.append("url = ")
            it.appendQuoted(repoUrl)
          }
          s.line { it.append(configureAction) }
        }
      }
    }

    private fun renderKotlin(scribe: Scribe): String {
      return if (configureAction == null) {
        scribe.line { s ->
          s.append("maven(url = ")
          s.appendQuoted(repoUrl)
          s.append(")")
        }
      } else {
        scribe.block("maven(url = $repoUrl)") { s ->
          s.line { it.append(configureAction) }
        }
      }
    }

    public override fun withConfigureAction(configureAction: String): Url {
      return copy(configureAction = configureAction)
    }
  }

  public fun asRepositories(): Repositories = Repositories(this)

  public companion object {
    @JvmField public val JITPACK: Url = ofMaven("https://jitpack.io")
    @JvmField public val GOOGLE: Method = Method("google")
    @JvmField public val GRADLE_PLUGIN_PORTAL: Method = Method("gradlePluginPortal")
    @JvmField public val MAVEN_CENTRAL: Method = Method("mavenCentral")
    @JvmField public val MAVEN_LOCAL: Method = Method("mavenLocal")
    @JvmField public val SNAPSHOTS: Url = ofMaven("https://central.sonatype.com/repository/maven-snapshots/")
    @JvmField public val LIBS: FlatDir = FlatDir("libs")

    /**
     * The repository for local projects if you're using the plugin `com.autonomousapps.testkit`. If not, a broken,
     * unusable repo.
     */
    @JvmField public val FUNC_TEST: Url = ofMaven(FUNC_TEST_REPO)

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
      MAVEN_CENTRAL,
      GOOGLE,
    )

    @JvmField
    public val DEFAULT_PLUGINS: List<Repository> = listOf(
      FUNC_TEST,
      GRADLE_PLUGIN_PORTAL,
      MAVEN_CENTRAL,
      GOOGLE
    )

    @JvmStatic
    public fun ofMaven(repoUrl: String): Url = Url(repoUrl.escape())
  }
}
