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
                    fun printHello() {
                      println("hello")
                    }
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
            
            class More
          '''
        )
      // still part of commonMain (a nonstandard dir)
        .withSourceSet('more')
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
    Advice.ofRemove(moduleCoordinates('androidx.compose.runtime:runtime:1.11.4'), 'commonMainImplementation'),
    Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0'), 'commonMainApi'),
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
            api('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0')
          }
        }
      }'''.stripIndent()
  }
}
