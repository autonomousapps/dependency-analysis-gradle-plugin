// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.render

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.*
import com.autonomousapps.kit.gradle.Dependency.Companion.implementation
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ScribeTestKotlin {

  private val scribe = Scribe(
    dslKind = GradleProject.DslKind.KOTLIN,
    indent = 2,
  )

  @Nested inner class RepositoriesTest {
    @Test fun `can render single repository`() {
      // Given
      val repo = Repository.GOOGLE

      // When
      val text = repo.render(scribe)

      // Then
      assertThat(text).isEqualTo("google()\n")
    }

    @Test fun `can render repositories block`() {
      // Given
      val repositories = Repositories(
        Repository.GOOGLE,
        Repository.MAVEN_CENTRAL,
        Repository.SNAPSHOTS,
      )

      // When
      val text = repositories.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
        repositories {
          google()
          mavenCentral()
          maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        }
        
      """.trimIndent()
      )
    }
  }

  @Nested inner class SettingsTest {
    @Test fun `can render dependencyResolutionManagement block`() {
      // Given
      val repositories = Repositories(Repository.GOOGLE, Repository.MAVEN_CENTRAL)
      val dependencyResolutionManagement = DependencyResolutionManagement(repositories)

      // When
      val text = dependencyResolutionManagement.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
        dependencyResolutionManagement {
          repositories {
            google()
            mavenCentral()
          }
        }
        
      """.trimIndent()
      )
    }

    @Test fun `can render settings script in one pass`() {
      // Given
      val imports = Imports.of("org.magic", "turtles")
      val pluginManagement = PluginManagement(Repositories.DEFAULT_PLUGINS)
      val dependencyResolutionManagement = DependencyResolutionManagement(
        Repositories(Repository.GOOGLE, Repository.MAVEN_CENTRAL)
      )
      val rootProjectName = "test-project"
      val settings = SettingsScript(
        imports = imports,
        pluginManagement = pluginManagement,
        dependencyResolutionManagement = dependencyResolutionManagement,
        rootProjectName = rootProjectName,
        plugins = Plugins(
          Plugin("com.autonomousapps.dependency-analysis", "1.25.0"),
          Plugin("com.gradle.enterprise", "3.11.4")
        ),
        subprojects = setOf("a", "b"),
        // TODO(tsr): need withGroovy() and withKotlin() methods.
        //   additions = """
        //   ext.magic = 'octarine'
        //   ext.meaningOfLife = 42
        // """.trimIndent()
      )

      // When
      val text = settings.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
        import org.magic
        import turtles
        
        pluginManagement {
          repositories {
            maven { url = uri("") }
            gradlePluginPortal()
            mavenCentral()
            google()
          }
        }
        
        plugins {
          id("com.autonomousapps.dependency-analysis") version "1.25.0"
          id("com.gradle.enterprise") version "3.11.4"
        }
        
        dependencyResolutionManagement {
          repositories {
            google()
            mavenCentral()
          }
        }
        
        rootProject.name = "$rootProjectName"
        
        include(":a")
        include(":b")
        
      """.trimIndent()
      )
    }
  }

  @Nested inner class DependenciesTest {
    @Test fun `can render an external dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(\"com:foo:1.0\")\n")
    }

    @Test fun `can render a local dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(project(\":foo\"))\n")
    }

    @Test fun `can render function call`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "dagger()",
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(dagger())\n")
    }

    @Test fun `can render custom notation`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "deps.foo.bar",
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(deps.foo.bar)\n")
    }

    @Test fun `can render dependency with nonstandard extension`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        ext = "aar",
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(name = \"com:foo:1.0\", ext = \"aar\")\n")
    }

    @Test fun `can render testFixtures to external dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        capability = "test-fixtures"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(testFixtures(\"com:foo:1.0\"))\n")
    }

    @Test fun `can render testFixtures to local dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
        capability = "test-fixtures"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(testFixtures(project(\":foo\")))\n")
    }

    @Test fun `can render platform to external dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        capability = "platform"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(platform(\"com:foo:1.0\"))\n")
    }

    @Test fun `can render platform to local dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
        capability = "platform"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(platform(project(\":foo\")))\n")
    }

    @Test fun `can render enforcedPlatform to external dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        capability = "enforcedPlatform"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(enforcedPlatform(\"com:foo:1.0\"))\n")
    }

    @Test fun `can render enforcedPlatform to local dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
        capability = "enforcedPlatform"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation(enforcedPlatform(project(\":foo\")))\n")
    }

    @Test fun `can render custom capability to external dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        capability = "myFeature"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text)
        .isEqualTo("implementation(\"com:foo:1.0\") { capabilities { requireCapabilities(\"myFeature\") } }\n")
    }

    @Test fun `can render custom capability to internal dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
        capability = "myFeature"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        "implementation(project(\":foo\")) { capabilities { requireCapabilities(\"myFeature\") } }\n"
      )
    }

    @Test fun `can render custom capability to dependency with nonstandard extension`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = "com:foo:1.0",
        capability = "myFeature",
        ext = "aar"
      )

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        "implementation(name = \"com:foo:1.0\", ext = \"aar\") { capabilities { requireCapabilities(\"myFeature\") } }\n"
      )
    }

    @Test fun `can render dependencies block`() {
      // Given
      val dependencies =
        Dependencies(
          Dependency("antlr", "org.antlr:antlr4:4.8-1"),
          implementation("commons-io:commons-io:2.6"),
        )

      // When
      val text = dependencies.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          dependencies {
            antlr("org.antlr:antlr4:4.8-1")
            implementation("commons-io:commons-io:2.6")
          }
          
        """.trimIndent()
      )
    }
  }

  @Nested inner class BuildscriptBlockTest {
    @Test fun `can render buildscript block`() {
      // Given
      val repositories = Repositories.DEFAULT_PLUGINS
      val dependencies = Dependencies(
        Dependency("antlr", "org.antlr:antlr4:4.8-1"),
        implementation("commons-io:commons-io:2.6")
      )
      val buildscriptBlock = BuildscriptBlock(repositories, dependencies)

      // When
      val text = buildscriptBlock.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          buildscript {
            repositories {
              maven { url = uri("") }
              gradlePluginPortal()
              mavenCentral()
              google()
            }
            dependencies {
              antlr("org.antlr:antlr4:4.8-1")
              implementation("commons-io:commons-io:2.6")
            }
          }
          
        """.trimIndent()
      )
    }
  }

  @Nested inner class BuildScriptTest {
    @Test fun `can render a build script that has a group without a version`() {
      // Given
      val group = "com.group"
      val buildScript = BuildScript(
        group = group,
      )

      // When
      val text = buildScript.render(scribe)

      // Then
      assertThat(text).isEqualTo("group = \"$group\"\n\n")
    }

    @Test fun `can render a build script that has a version without a group`() {
      // Given
      val version = "1.0"
      val buildScript = BuildScript(
        version = version,
      )

      // When
      val text = buildScript.render(scribe)

      // Then
      assertThat(text).isEqualTo("version = \"$version\"\n\n")
    }

    @Test fun `can render a build script`() {
      // Given
      val imports = Imports.of("org.magic", "turtles")
      val buildscriptBlock = BuildscriptBlock(
        Repositories.DEFAULT_PLUGINS,
        Dependencies(
          Dependency("antlr", "org.antlr:antlr4:4.8-1"),
          implementation("commons-io:commons-io:2.6"),
        )
      )
      val plugins = Plugins(Plugin.application, Plugin.groovy)
      val group = "com.group"
      val version = "1.0"
      val dependencies = Dependencies(Dependency("api", ":magic"))
      val androidBlock = AndroidBlock(
        namespace = "ankh.morpork"
      )

      val buildScript = BuildScript(
        imports = imports,
        buildscript = buildscriptBlock,
        plugins = plugins,
        group = group,
        version = version,
        dependencies = dependencies,
        android = androidBlock,
        sourceSets = SourceSets.ofNames("corpos", "nomad", "streetKid"),
        java = Java.ofFeatures(Feature.ofName("cyber"), Feature.ofName("punk")),
        // TODO(tsr): withGroovy() and withKotlin()
        // additions = """
        //   ext.magic = "octarine"
        //
        // """.trimIndent()
      )

      // When
      val text = buildScript.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          import org.magic
          import turtles
          
          buildscript {
            repositories {
              maven { url = uri("") }
              gradlePluginPortal()
              mavenCentral()
              google()
            }
            dependencies {
              antlr("org.antlr:antlr4:4.8-1")
              implementation("commons-io:commons-io:2.6")
            }
          }
          
          plugins {
            id("application")
            id("groovy")
          }
          
          group = "$group"
          version = "$version"
          
          android {
            namespace = "ankh.morpork"
            compileSdk = 34
            defaultConfig {
              applicationId = "com.example"
              minSdk = 21
              targetSdk = 29
              versionCode = 1
              versionName = "1.0"
            }
            compileOptions {
              sourceCompatibility = JavaVersion.VERSION_1_8
              targetCompatibility = JavaVersion.VERSION_1_8
            }
          }
          
          sourceSets {
            create("cyber")
            create("punk")
            create("corpos")
            create("nomad")
            create("streetKid")
          }
          
          java {
            registerFeature("cyber") {
              usingSourceSet(sourceSets["cyber"])
            }
            registerFeature("punk") {
              usingSourceSet(sourceSets["punk"])
            }
          }
          
          dependencies {
            api(project(":magic"))
          }

        """.trimIndent()
      )
    }

    @Test fun `can render custom android content`() {
      // Given
      val buildScript = BuildScript(
        android = AndroidBlock.Builder().apply {
          withKotlin("custom { config = true }")
        }.build(),
        usesKotlin = true,
      )

      // When
      val text = buildScript.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          android {
            compileSdk = 34
            defaultConfig {
              applicationId = "com.example"
              minSdk = 21
              targetSdk = 29
              versionCode = 1
              versionName = "1.0"
            }
            compileOptions {
              sourceCompatibility = JavaVersion.VERSION_1_8
              targetCompatibility = JavaVersion.VERSION_1_8
            }
            custom { config = true }
          }
        
        
        """.trimIndent()
      )
    }
  }

  @Nested inner class PluginsTest {
    @Test fun `can render a plugin with a version`() {
      // Given
      val plugin = Plugin("magic", "1.0")

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id(\"magic\") version \"1.0\"\n")
    }

    @Test fun `can render a plugin without a version`() {
      // Given
      val plugin = Plugin("magic")

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id(\"magic\")\n")
    }

    @Test fun `can render a plugin with a version and apply false`() {
      // Given
      val plugin = Plugin("magic", "1.0", false)

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id(\"magic\") version \"1.0\" apply false\n")
    }

    @Test fun `can render a plugin without a version and apply false`() {
      // Given
      val plugin = Plugin(id = "magic", apply = false)

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id(\"magic\") apply false\n")
    }

    @Test fun `can render plugins block`() {
      // Given
      val plugins = Plugins(
        Plugin("magic", "1.0"),
        Plugin("meaning-of-life", "2.0"),
      )

      // Expect
      assertThat(plugins.render(scribe)).isEqualTo(
        """
          plugins {
            id("magic") version "1.0"
            id("meaning-of-life") version "2.0"
          }
          
        """.trimIndent()
      )
    }
  }
}
