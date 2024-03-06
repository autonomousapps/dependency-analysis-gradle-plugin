// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Plugin @JvmOverloads constructor(
  public val id: String,
  public val version: String? = null,
  public val apply: Boolean = true,
) : Element.Line {

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.line { s ->
    s.append("id ")
    s.appendQuoted(id)
    version?.let { v ->
      s.append(" version ")
      s.appendQuoted(v)
    }
    if (!apply) {
      s.append(" apply false")
    }
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.line { s ->
    s.append("id(")
    s.appendQuoted(id)
    s.append(")")
    version?.let { v ->
      s.append(" version ")
      s.appendQuoted(v)
    }
    if (!apply) {
      s.append(" apply false")
    }
  }

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun of(
      id: String,
      version: String? = null,
      apply: Boolean = true,
    ): Plugin = Plugin(id, version, apply)

    /*
     * Gradle core plugins.
     */

    @JvmStatic public val antlr: Plugin = Plugin("antlr")
    @JvmStatic public val application: Plugin = Plugin("application")
    @JvmStatic public val groovy: Plugin = Plugin("groovy")
    @JvmStatic public val groovyGradle: Plugin = Plugin("groovy-gradle-plugin")
    @JvmStatic public val java: Plugin = Plugin("java")
    @JvmStatic public val javaGradle: Plugin = Plugin("java-gradle-plugin")
    @JvmStatic public val javaLibrary: Plugin = Plugin("java-library")
    @JvmStatic public val javaTestFixtures: Plugin = Plugin("java-test-fixtures")
    @JvmStatic public val scala: Plugin = Plugin("scala")
    @JvmStatic public val war: Plugin = Plugin("war")
  }
}
