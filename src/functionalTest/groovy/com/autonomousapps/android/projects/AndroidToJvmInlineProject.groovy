// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.JvmToolchain
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class AndroidToJvmInlineProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidToJvmInlineProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer', 'com.example.consumer') { l ->
        l.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock(true, 'com.example.consumer')
          bs.dependencies = [
            project('implementation', ':producer')
          ]
        }
        l.sources = consumerSources
      }
      .withSubproject('producer') { l ->
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.kotlin { k ->
            k.jvmToolchain = JvmToolchain.DEFAULT
          }
        }
        l.sources = producerSources
      }
      .write()
  }

  private consumerSources = [
    Source.kotlin('''
        package com.example.consumer
                
        import com.example.producer.magic
              
        class Consumer {
          fun useMagic(): Int {
            return listOf("meaning of life").magic()
          }
        }
      ''')
      .withPath('com/example/consumer', 'Consumer')
      .build()
  ]

  private producerSources = [
    Source.kotlin('''
        package com.example.producer
        
        inline fun <reified T : Any> List<out T>.magic(): Int = 42
      ''')
      .withPath('com/example/producer', 'Producer')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer'),
  ]
}
