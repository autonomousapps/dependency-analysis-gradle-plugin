package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

final class DataBindingWithExpressionsProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  DataBindingWithExpressionsProject(String agpVersion) {
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
        bs.plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = dependencies
        bs.additions = "android.buildFeatures.dataBinding true"
      }
      app.manifest = AndroidManifest.defaultLib("com.example.app")
      app.sources = sources
      app.withFile('src/main/res/layout/main_activity.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:text="@{`Text`}" />

          </layout>
        """.stripIndent()
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

  private List<Dependency> dependencies = [
    Dependency.kotlinStdLib("implementation"),
    Dependency.appcompat("implementation"),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    if (AgpVersion.version(agpVersion) >= AgpVersion.version('7.4.0')) {
      [
        projectAdviceForDependencies(':app', [
          Advice.ofRemove(moduleCoordinates('org.jetbrains.kotlin:kotlin-stdlib:1.6.10'), 'implementation'),
        ] as Set<Advice>),
      ]
    } else {
      [emptyProjectAdviceFor(':app')]
    }
  }
}
