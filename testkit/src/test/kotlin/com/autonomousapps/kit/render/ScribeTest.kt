package com.autonomousapps.kit.render

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.gradle.*
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ScribeTest {

  private val scribe = Scribe(
    dslKind = DslKind.GROOVY,
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
      val repositories = Repositories(Repository.GOOGLE, Repository.MAVEN_CENTRAL)

      // When
      val text = repositories.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
        repositories {
          google()
          mavenCentral()
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
      val pluginManagement = PluginManagement(Repositories.DEFAULT_PLUGINS)
      val dependencyResolutionManagement = DependencyResolutionManagement(Repositories.DEFAULT_DEPENDENCIES)
      val rootProjectName = "test-project"
      val settings = SettingsScript(
        pluginManagement = pluginManagement,
        dependencyResolutionManagement = dependencyResolutionManagement,
        rootProjectName = rootProjectName,
        plugins = Plugins(
          Plugin("com.autonomousapps.dependency-analysis", "1.25.0"),
          Plugin.gradleEnterprisePlugin
        ),
        subprojects = setOf("a", "b"),
        additions = """
        ext.magic = 'octarine'
        ext.meaningOfLife = 42
      """.trimIndent()
      )

      // When
      val text = settings.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
        pluginManagement {
          repositories {
            gradlePluginPortal()
            mavenCentral()
          }
        }
        
        plugins {
          id 'com.autonomousapps.dependency-analysis' version '1.25.0'
          id 'com.gradle.enterprise' version '3.11.4'
        }
        
        dependencyResolutionManagement {
          repositories {
            google()
            mavenCentral()
          }
        }
        
        rootProject.name = '$rootProjectName'
        
        include ':a'
        include ':b'
        
        ext.magic = 'octarine'
        ext.meaningOfLife = 42
        
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
      assertThat(text).isEqualTo("implementation 'com:foo:1.0'\n")
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
      assertThat(text).isEqualTo("implementation project(':foo')\n")
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
      assertThat(text).isEqualTo("implementation dagger()\n")
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
      assertThat(text).isEqualTo("implementation deps.foo.bar\n")
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
      assertThat(text).isEqualTo("implementation(name: 'com:foo:1.0', ext: 'aar')\n")
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
      assertThat(text).isEqualTo("implementation testFixtures('com:foo:1.0')\n")
    }

    @Test fun `can render testFixtures to local dependency`() {
      // Given
      val dependency = Dependency(
        configuration = "implementation",
        dependency = ":foo",
        capability = "test-fixtures"
      )
      val scribe = Scribe()

      // When
      val text = dependency.render(scribe)

      // Then
      assertThat(text).isEqualTo("implementation testFixtures(project(':foo'))\n")
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
        .isEqualTo("implementation('com:foo:1.0') { capabilities { requireCapabilities('myFeature') } }\n")
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
        "implementation(project(':foo')) { capabilities { requireCapabilities('myFeature') } }\n"
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
        "implementation(name: 'com:foo:1.0', ext: 'aar') { capabilities { requireCapabilities('myFeature') } }\n"
      )
    }

    @Test fun `can render dependencies block`() {
      // Given
      val dependencies = Dependencies(Dependency.antlr(), Dependency.commonsIO("implementation"))

      // When
      val text = dependencies.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          dependencies {
            antlr 'org.antlr:antlr4:4.8-1'
            implementation 'commons-io:commons-io:2.6'
          }
          
        """.trimIndent()
      )
    }
  }

  @Nested inner class BuildscriptBlockTest {
    @Test fun `can render buildscript block`() {
      // Given
      val repositories = Repositories.DEFAULT_PLUGINS
      val dependencies = Dependencies(Dependency.antlr(), Dependency.commonsIO("implementation"))
      val buildscriptBlock = BuildscriptBlock(repositories, dependencies)

      // When
      val text = buildscriptBlock.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          buildscript {
            repositories {
              gradlePluginPortal()
              mavenCentral()
            }
            dependencies {
              antlr 'org.antlr:antlr4:4.8-1'
              implementation 'commons-io:commons-io:2.6'
            }
          }
          
        """.trimIndent()
      )
    }
  }

  @Nested inner class BuildScriptTest {
    @Test fun `can render a build script`() {
      // Given
      val buildscriptBlock = BuildscriptBlock(
        Repositories.DEFAULT_PLUGINS,
        Dependencies(Dependency.antlr(), Dependency.commonsIO("implementation"))
      )
      val plugins = Plugins(Plugin.applicationPlugin, Plugin.groovyPlugin)
      val dependencies = Dependencies(Dependency("api", ":magic"))
      val androidBlock = AndroidBlock(
        namespace = "ankh.morpork"
      )

      val buildScript = BuildScript(
        buildscript = buildscriptBlock,
        plugins = plugins,
        dependencies = dependencies,
        android = androidBlock,
        sourceSets = SourceSets.ofNames("corpos", "nomad", "streetKid"),
        java = Java.ofFeatures(Feature.ofName("cyber"), Feature.ofName("punk")),
        additions = """
          ext.magic = "octarine"
          
        """.trimIndent()
      )

      // When
      val text = buildScript.render(scribe)

      // Then
      assertThat(text).isEqualTo(
        """
          buildscript {
            repositories {
              gradlePluginPortal()
              mavenCentral()
            }
            dependencies {
              antlr 'org.antlr:antlr4:4.8-1'
              implementation 'commons-io:commons-io:2.6'
            }
          }
          
          plugins {
            id 'application'
            id 'groovy'
          }
          
          android {
            namespace 'ankh.morpork'
            compileSdkVersion 33
            defaultConfig {
              applicationId "com.example"
              minSdkVersion 21
              targetSdkVersion 29
              versionCode 1
              versionName "1.0"
            }
            compileOptions {
              sourceCompatibility JavaVersion.VERSION_1_8
              targetCompatibility JavaVersion.VERSION_1_8
            }
          }
          
          sourceSets {
            cyber
            punk
            corpos
            nomad
            streetKid
          }
          
          java {
            registerFeature("cyber") {
              usingSourceSet(sourceSets.cyber)
            }
            registerFeature("punk") {
              usingSourceSet(sourceSets.punk)
            }
          }
          
          ext.magic = "octarine"

          dependencies {
            api project(':magic')
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
      assertThat(plugin.render(scribe)).isEqualTo("id 'magic' version '1.0'\n")
    }

    @Test fun `can render a plugin without a version`() {
      // Given
      val plugin = Plugin("magic")

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id 'magic'\n")
    }

    @Test fun `can render a plugin with a version and apply false`() {
      // Given
      val plugin = Plugin("magic", "1.0", false)

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id 'magic' version '1.0' apply false\n")
    }

    @Test fun `can render a plugin without a version and apply false`() {
      // Given
      val plugin = Plugin(id = "magic", apply = false)

      // Expect
      assertThat(plugin.render(scribe)).isEqualTo("id 'magic' apply false\n")
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
            id 'magic' version '1.0'
            id 'meaning-of-life' version '2.0'
          }
          
        """.trimIndent()
      )
    }
  }
}
