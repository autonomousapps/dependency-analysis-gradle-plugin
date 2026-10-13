// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class ReplaceWithTransitiveProject extends AbstractKmpProject {

  final GradleProject gradleProject

  ReplaceWithTransitiveProject() {
    super(KOTLIN_VERSION)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(plugins.dependencyAnalysis, plugins.kotlinMultiplatformNoApply)
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = kmpLibrary
          bs.kotlinKmp { k ->
            // https://github.com/veyndan/dependency-analysis-gradle-plugin-playground/blob/1784/build.gradle.kts
            //jvm()               // This is what `KotlinJvmTarget.default()` does.
            //iosArm64()          // TODO(tsr): no support yet
            //iosSimulatorArm64() // TODO(tsr): no support yet
            k.jvmTarget = KotlinJvmTarget.default()
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  // Should be `api('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0')`
                  implementation('androidx.compose.runtime:runtime:1.11.4'),

                  // Unused, but provides commons-io
                  commonsCompress('implementation'),

                  // Unused, but provides commons-text
                  commonsConfiguration2('implementation'),
                )
              }
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(
                  // Unused, but provides used okio
                  okHttp('implementation')
                )
              }
            }
          }

          // To ensure our source-finding code accounts for nonstandard and generated directories
          bs.withKotlin(
            '''\
            val generatorTask = project.tasks.register("genSource") {
              val outputDirectory = project.layout.buildDirectory.dir("gen/kotlin")
              outputs.dir(outputDirectory)
              doLast {
                // no package
                outputDirectory.get().file("generated.kt").asFile.writeText(
                  """
                    import org.apache.commons.text.CaseUtils // commons-text
                  
                    fun printHello() {
                      println("hello")
                    }
                    
                    fun toCamelCase(s: String) = CaseUtils.toCamelCase(s, false)
                  """.trimIndent()
                )
                
                // with package
                val out = outputDirectory.get().file("a/b/generated.kt").asFile
                out.parentFile.mkdirs()
                out.writeText(
                  """
                    package a.b
                  
                    fun printHello() {
                      println("hello")
                    }
                  """.trimIndent()
                )
              }
            }
            
            kotlin.sourceSets.commonMain.configure {
              generatedKotlin.srcDir(generatorTask)
              kotlin.srcDir(project.layout.projectDirectory.dir("src/more/kotlin"))
            }'''.stripIndent()
          )
        }
      }
      .write()
  }

  private static List<Source> consumerSources() {
    return [
      /* commonMain */
      Source
        .kotlin(
          '''
            package common.main
            
            // Provided transitively: `androidx.compose.runtime:runtime -> org.jetbrains.kotlinx:kotlinx-coroutines-core`
            import kotlinx.coroutines.flow.Flow
            
            data class Foo(val bar: Flow<Int>)
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      // This only exists to exercise ListSourceFilesTask
      Source
        .kotlin(
          '''
            package common.main
            
            class Bar
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package common.main.more
            
            import org.apache.commons.io.output.NullWriter // commons-io
            
            class More {
              fun nullWriter() = NullWriter()
            }
          '''
        )
      // still part of commonMain (a nonstandard dir)
        .withSourceSet('more')
        .build(),

      /* commonTest */
      Source
        .kotlin(
          '''
            package common.test
            
            import okio.Buffer
            
            class Test {
              // Part of the "ABI" (but tests don't have an ABI)
              fun buffer() = Buffer()
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  /*
   * buildHealth
   */

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    // commonMain...
    // ... (default dir) androidx.compose.runtime and kotlinx-coroutines-core
    Advice.ofRemove(moduleCoordinates('androidx.compose.runtime:runtime:1.11.4'), 'commonMainImplementation'),
    Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0'), 'commonMainApi'),
    // ... (non-standard dir) commons-compress and commons-io
    Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-compress:1.28.0'), 'commonMainImplementation'),
    Advice.ofAdd(moduleCoordinates('commons-io:commons-io:2.20.0'), 'commonMainApi'),
    // ... (generated source) commons-configuration2 and commons-text
    Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-configuration2:2.14.0'), 'commonMainImplementation'),
    Advice.ofAdd(moduleCoordinates('org.apache.commons:commons-text:1.15.0'), 'commonMainImplementation'),

    // commonTest...
    // okhttp and okio
    Advice.ofRemove(moduleCoordinates('com.squareup.okhttp3:okhttp:4.6.0'), 'commonTestImplementation'),
    Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio:2.6.0'), 'commonTestImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]

  /*
   * fixDependencies
   */

  final String actualBuildscriptContent() {
    return gradleProject.projectDir('consumer').resolve('build.gradle.kts').text.trim()
  }

  final String expectedBuildScriptContent() {
    return '''\
      plugins {
        id("org.jetbrains.kotlin.multiplatform")
        id("com.autonomousapps.dependency-analysis")
      }
      
      kotlin {
        jvm()
        sourceSets {
          commonMain.dependencies {
            api("commons-io:commons-io:2.20.0")
            implementation("org.apache.commons:commons-text:1.15.0")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
          }
          commonTest.dependencies {
            implementation("com.squareup.okio:okio:2.6.0")
          }
        }
      }
      
      val generatorTask = project.tasks.register("genSource") {
        val outputDirectory = project.layout.buildDirectory.dir("gen/kotlin")
        outputs.dir(outputDirectory)
        doLast {
          // no package
          outputDirectory.get().file("generated.kt").asFile.writeText(
            """
              import org.apache.commons.text.CaseUtils // commons-text
      
              fun printHello() {
                println("hello")
              }
      
              fun toCamelCase(s: String) = CaseUtils.toCamelCase(s, false)
            """.trimIndent()
          )
      
          // with package
          val out = outputDirectory.get().file("a/b/generated.kt").asFile
          out.parentFile.mkdirs()
          out.writeText(
            """
              package a.b
      
              fun printHello() {
                println("hello")
              }
            """.trimIndent()
          )
        }
      }
      
      kotlin.sourceSets.commonMain.configure {
        generatedKotlin.srcDir(generatorTask)
        kotlin.srcDir(project.layout.projectDirectory.dir("src/more/kotlin"))
      }'''.stripIndent()
  }
}
