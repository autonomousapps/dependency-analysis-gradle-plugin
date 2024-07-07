// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.internal.ensurePrefix
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Dependency @JvmOverloads constructor(
  public val configuration: String,
  private val dependency: String,
  private val ext: String? = null,
  private val capability: String? = null,
  private val isVersionCatalog: Boolean = false,
) : Element.Line {

  private val isProject = dependency.startsWith(":")

  public val identifier: String = if (isProject) dependency else dependency.substringBeforeLast(":")
  public val version: String? = if (isProject) null else dependency.substringAfterLast(":")

  // TODO(tsr): model this
  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.line { s ->
    val text = when {
      // project dependency
      dependency.startsWith(':') -> "$configuration project('$dependency')"
      // function call
      dependency.endsWith("()") -> "$configuration $dependency"
      // Some kind of custom notation
      !dependency.contains(":") -> "$configuration $dependency"
      // version catalog reference
      isVersionCatalog -> "$configuration $dependency"

      // normal dependency
      else -> {
        // normal external dependencies
        if (ext == null) "$configuration '$dependency'"
        // flat dependencies, e.g. in a libs/ dir
        else "$configuration(name: '$dependency', ext: '$ext')"
      }
    }.let {
      when {
        // Note: 'testFixtures("...")' is a shorthand for 'requireCapabilities("...-test-fixtures")'
        capability == "test-fixtures" -> {
          it.replace("$configuration ", "$configuration testFixtures(") + ")"
        }

        capability == "platform" -> {
          it.replace("$configuration ", "$configuration platform(") + ")"
        }

        capability == "enforcedPlatform" -> {
          it.replace("$configuration ", "$configuration enforcedPlatform(") + ")"
        }

        capability != null -> {
          if (it.startsWith("$configuration ")) {
            it.replace("$configuration ", "$configuration(") +
              ") { capabilities { requireCapabilities('$capability') } }"
          } else {
            "$it { capabilities { requireCapabilities('$capability') } }"
          }
        }

        else -> it
      }
    }

    s.append(text)
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.line { s ->
    val text = when {
      // project dependency
      dependency.startsWith(':') -> "$configuration(project(\"$dependency\"))"
      // function call
      dependency.endsWith("()") -> "$configuration($dependency)"
      // Some kind of custom notation
      !dependency.contains(":") -> "$configuration($dependency)"
      // version catalog reference
      isVersionCatalog -> "$configuration($dependency)"

      // normal dependency
      else -> {
        // normal external dependencies
        if (ext == null) "$configuration(\"$dependency\")"
        // flat dependencies, e.g. in a libs/ dir
        else "$configuration(name = \"$dependency\", ext = \"$ext\")"
      }
    }.let {
      when {
        // Note: 'testFixtures("...")' is a shorthand for 'requireCapabilities("...-test-fixtures")'
        capability == "test-fixtures" -> {
          it.replace("$configuration(", "$configuration(testFixtures(") + ")"
        }

        capability == "platform" -> {
          it.replace("$configuration(", "$configuration(platform(") + ")"
        }

        capability == "enforcedPlatform" -> {
          it.replace("$configuration(", "$configuration(enforcedPlatform(") + ")"
        }

        capability != null -> "$it { capabilities { requireCapabilities(\"$capability\") } }"

        else -> it
      }
    }

    s.append(text)
  }

  override fun toString(): String {
    error("don't call toString()")
  }

  public companion object {

    @JvmOverloads
    @JvmStatic
    public fun annotationProcessor(dependency: String, capability: String? = null): Dependency {
      return Dependency("annotationProcessor", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun api(dependency: String, capability: String? = null): Dependency {
      return Dependency("api", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun classpath(dependency: String, capability: String? = null): Dependency {
      return Dependency("classpath", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun compileOnly(dependency: String, capability: String? = null): Dependency {
      return Dependency("compileOnly", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun compileOnlyApi(dependency: String, capability: String? = null): Dependency {
      return Dependency("compileOnlyApi", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun implementation(dependency: String, capability: String? = null): Dependency {
      return Dependency("implementation", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun runtimeOnly(dependency: String, capability: String? = null): Dependency {
      return Dependency("runtimeOnly", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun testCompileOnly(dependency: String, capability: String? = null): Dependency {
      return Dependency("testCompileOnly", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun testImplementation(dependency: String, capability: String? = null): Dependency {
      return Dependency("testImplementation", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun testRuntimeOnly(dependency: String, capability: String? = null): Dependency {
      return Dependency("testRuntimeOnly", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun project(configuration: String, path: String, capability: String? = null): Dependency {
      return Dependency(configuration, path.ensurePrefix(), capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun raw(configuration: String, dependency: String, capability: String? = null): Dependency {
      check(!dependency.contains(":")) { "Not meant for normal dependencies. Was '$dependency'." }
      return Dependency(configuration, dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun versionCatalog(configuration: String, ref: String, capability: String? = null): Dependency {
      return Dependency(
        configuration = configuration,
        dependency = ref,
        isVersionCatalog = true,
        capability = capability,
      )
    }

    /*
     * Plugin classpaths
     */

    @JvmStatic
    public fun androidPlugin(version: String = "3.6.3"): Dependency {
      return Dependency("classpath", "com.android.tools.build:gradle:$version")
    }
  }
}
