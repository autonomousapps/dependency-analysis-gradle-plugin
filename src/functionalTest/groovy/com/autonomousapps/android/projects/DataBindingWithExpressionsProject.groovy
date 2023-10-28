package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.appcompat

final class DataBindingWithExpressionsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DataBindingWithExpressionsProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { app ->
      app.withBuildScript { bs ->
        bs.plugins = [Plugin.androidApp, Plugin.kotlinAndroid, Plugin.kapt]
        bs.android = androidAppBlock(true, 'com.example.app')
        bs.dependencies = [
          appcompat("implementation")
        ]
        bs.additions = "android.buildFeatures.dataBinding true"
      }
      app.manifest = appManifest('com.example.app')
      app.sources = sources
      app.withFile('src/main/res/layout/main_activity.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:text="@{`Text`}" />

          </layout>""".stripIndent()
      )
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example/app',
      """
        package com.example.app
        
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [emptyProjectAdviceFor(':app')]
}
