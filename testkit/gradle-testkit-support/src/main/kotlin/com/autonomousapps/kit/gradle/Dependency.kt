// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.internal.ensurePrefix
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public data class Dependency @JvmOverloads constructor(
  public val configuration: String,
  private val dependency: String,
  // TODO(tsr): missing classifier. https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/DependencyArtifact.html#getClassifier()
  private val ext: String? = null,
  private val capability: String? = null,
  private val isVersionCatalog: Boolean = false,
) : Element.Line {

  private val isProject = dependency.startsWith(":")

  public val identifier: String = if (isProject) dependency else dependency.substringBeforeLast(":")
  public val version: String? = if (isProject) null else dependency.substringAfterLast(":")

  /**
   * Convert into a [dependency] on the target's `testFixtures`.
   *
   * @see <a href="https://docs.gradle.org/current/userguide/java_testing.html">Java Testing</a>
   */
  public fun onTestFixtures(): Dependency {
    return copy(capability = CAPABILITY_TEST_FIXTURES)
  }

  /**
   * Convert into a [dependency] with extension [ext].
   *
   * @see <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/DependencyArtifact.html#getExtension()">DependencyArtifact::getExtension</a>
   */
  public fun ext(ext: String): Dependency {
    return copy(ext = ext)
  }

  /** Specify that this [Dependency] uses a version catalog accessor. */
  public fun versionCatalog(): Dependency {
    return copy(isVersionCatalog = true)
  }

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
        capability == CAPABILITY_TEST_FIXTURES -> {
          it.replace("$configuration ", "$configuration testFixtures(") + ")"
        }

        capability == CAPABILITY_PLATFORM -> {
          it.replace("$configuration ", "$configuration platform(") + ")"
        }

        capability == CAPABILITY_ENFORCED_PLATFORM -> {
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

    @JvmStatic public val CAPABILITY_ENFORCED_PLATFORM: String = "enforcedPlatform"
    @JvmStatic public val CAPABILITY_PLATFORM: String = "platform"
    @JvmStatic public val CAPABILITY_TEST_FIXTURES: String = "test-fixtures"

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
    public fun testFixturesImplementation(dependency: String, capability: String? = null): Dependency {
      return Dependency("testFixturesImplementation", dependency, capability = capability)
    }

    @JvmOverloads
    @JvmStatic
    public fun testFixturesApi(dependency: String, capability: String? = null): Dependency {
      return Dependency("testFixturesApi", dependency, capability = capability)
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
    public fun androidPlugin(version: String): Dependency {
      return Dependency("classpath", "com.android.tools.build:gradle:$version")
    }
  }
}
