package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class KmpAndroidProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  KmpAndroidProject(String agpVersion) {
    super(agpVersion)
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
      .withAndroidSubproject('app') { app ->
        app.manifest = AndroidManifest.simpleApp()
        app.sources = sourcesConsumer
        app.styles = null
        app.strings = null
        app.colors = null
        app.withBuildScript { bs ->
          bs.android = defaultAndroidAppBlock()
          bs.plugins = [
            Plugins.androidApp,
            Plugins.kotlinAndroid
          ]
          bs.dependencies = [
            // The artifact that is actually used is foundation-android
            implementation('androidx.compose.foundation:foundation:1.6.0-alpha06')
          ]
        }
      }.write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.KOTLIN, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer
        
        import androidx.compose.foundation.isSystemInDarkTheme
        
        class Consumer {
          fun fancyCode() {
            val darkTheme: Boolean = isSystemInDarkTheme()
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app'),
  ]
}
