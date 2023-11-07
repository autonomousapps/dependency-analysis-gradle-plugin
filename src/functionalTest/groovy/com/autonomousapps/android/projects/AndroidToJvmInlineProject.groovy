package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Kotlin
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
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.gradleProperties = GradleProperties.minimalAndroidProperties()
        root.withBuildScript { bs ->
          bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        }
      }
      .withAndroidLibProject('consumer', 'com.example.consumer') { l ->
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroid]
          bs.android = androidLibBlock(true, 'com.example.consumer')
          bs.dependencies = [
            project('implementation', ':producer')
          ]
        }
        l.sources = consumerSources
      }
      .withSubproject('producer') { l ->
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
          bs.kotlin = Kotlin.DEFAULT
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
