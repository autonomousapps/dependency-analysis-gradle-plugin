// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class FileFacadeProject extends AbstractKmpProject {

  final GradleProject gradleProject

  FileFacadeProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(
            plugins.dependencyAnalysis,
            plugins.kotlinMultiplatformNoApply,
            plugins.androidKmpLibNoApply,
          )
        }
      }
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.consumer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  implementation(':producer'),
                  implementation(':producer2'),
                  implementation('com.squareup.okio:okio:3.16.4'),
                )
              }
            }
          }
          bs.withKotlin(generatedSource())
        }
      }
      .withAndroidKmpLibProject('producer') { s ->
        s.sources = producerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.producer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
          }
        }
      }
      .withAndroidKmpLibProject('producer2') { s ->
        s.sources = producer2Sources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.producer2'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
          }
        }
      }
      .write()
  }

  // To ensure our source-finding code accounts for nonstandard and generated directories
  private static String generatedSource() {
    return '''\
      val generatorTask = project.tasks.register("genSource") {
      val outputDirectory = project.layout.buildDirectory.dir("gen/kotlin")
      outputs.dir(outputDirectory)
      doLast {
        // no package
        outputDirectory.get().file("generated.kt").asFile.writeText(
          """
            import producer2.common.main.whimper

            fun platform(): String = whimper("android")
          """.trimIndent()
        )
        
        // with package
        val out = outputDirectory.get().file("a/b/generated.kt").asFile
        out.parentFile.mkdirs()
        out.writeText(
          """
            package a.b

            import java.io.File
            import okio.source
            
            fun source(file: File) = file.source()
          """.trimIndent()
        )
      }
    }
    
    kotlin.sourceSets.commonMain.configure {
      generatedKotlin.srcDir(generatorTask)
      kotlin.srcDir(project.layout.projectDirectory.dir("src/more/kotlin"))
    }'''.stripIndent()
  }

  private static List<Source> consumerSources() {
    return [
      // Standard source file name
      Source
        .kotlin(
          '''
            package consumer.android.main

            import producer.common.main.shout

            class Platform {
              fun platform(): String = shout("android")
            }
          '''
        )
        .withSourceSet('androidMain')
        .build(),
    ]
  }

  private static List<Source> producerSources() {
    return [
      Source
        .kotlin(
          '''
            package producer.common.main

            inline fun shout(s: String): String = s.uppercase()
          '''
        )
        .withPath('producer.common.main', 'Shout')
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  private static List<Source> producer2Sources() {
    return [
      Source
        .kotlin(
          '''
            package producer2.common.main

            inline fun whimper(s: String): String = s.lowercase()
          '''
        )
        .withPath('producer2.common.main', 'Whimper')
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'commonMainImplementation', 'androidMainImplementation'),
    Advice.ofChange(moduleCoordinates('com.squareup.okio:okio:3.16.4'), 'commonMainImplementation', 'commonMainApi'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
    emptyProjectAdviceFor(':producer2'),
  ]
}
