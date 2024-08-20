package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

/**
 * In this app project, the only reference to the lib project is through a color resource. Does the plugin correctly say
 * that 'lib' is a used dependency?
 *
 * The only reference to the lib2 project is through an ID that lib2 provides.
 */
abstract class SettingsProject {

  static final class Success extends AbstractAndroidProject {
    final GradleProject gradleProject
    private final String agpVersion

    Success(String agpVersion) {
      super(agpVersion)
      this.agpVersion = agpVersion
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newAndroidSettingsProjectBuilder(agpVersion: agpVersion, withKotlin: true)
        .withAndroidSubproject('app') { app ->
          app.withBuildScript { bs ->
            bs.plugins = [Plugins.androidApp, Plugins.kotlinAndroidNoVersion]
            bs.android = defaultAndroidAppBlock()
            bs.dependencies = [
              project('implementation', ':lib'),
              project('implementation', ':lib2'),
              appcompat('implementation')
            ]
          }
          app.manifest = appManifest('com.example.app')
          app.sources = appSources
          app.styles = AndroidStyleRes.DEFAULT
          app.colors = AndroidColorRes.DEFAULT
          app.withFile('src/main/res/layout/message_layout.xml', '''\
        <?xml version="1.0" encoding="utf-8"?>
        <com.example.app.MessageLayout xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:app="http://schemas.android.com/apk/res-auto"
          xmlns:tools="http://schemas.android.com/tools"
          android:id="@id/message_layout"
          android:layout_width="match_parent"
          android:layout_height="wrap_content">
          
        </com.example.app.MessageLayout>'''.stripIndent()
          )
        }
        .withAndroidLibProject('lib', 'com.example.lib') { lib ->
          lib.withBuildScript { bs ->
            bs.plugins = [Plugins.androidLib]
            bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
          }
          lib.colors = AndroidColorRes.DEFAULT
          lib.manifest = libraryManifest('com.example.lib')
        }
        .withAndroidLibProject('lib2', 'com.example.lib2') { lib2 ->
          lib2.withBuildScript { bs ->
            bs.plugins = [Plugins.androidLib]
            bs.android = defaultAndroidLibBlock(false, 'com.example.lib2')
          }
          lib2.manifest = AndroidManifest.defaultLib('com.example.lib2')
          lib2.withFile('src/main/res/values/resources.xml', '''\
        <resources>
          <item name="message_layout" type="id"/>
        </resources>'''.stripIndent()
          )
        }
        .write()
    }

    private static final List<Source> appSources = [
      new Source(
        SourceType.KOTLIN, 'MainActivity.kt', 'com/example',
        '''\
        package com.example
        
        import androidx.appcompat.app.AppCompatActivity
        import com.example.lib.R
        
        class MainActivity : AppCompatActivity() {
          val i = R.color.colorPrimaryDark
        }'''.stripIndent()
      )
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<ProjectAdvice> expectedBuildHealth = [
      projectAdviceForDependencies(':app', downgradeKotlinStdlib()),
      emptyProjectAdviceFor(':lib'),
      emptyProjectAdviceFor(':lib2'),
    ]
  }

  static final class Failure extends AbstractAndroidProject {
    final GradleProject gradleProject
    private final String agpVersion

    Failure(String agpVersion) {
      super(agpVersion)
      this.agpVersion = agpVersion
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newAndroidGradleProjectBuilder(agpVersion)
        .withRootProject { r ->
          r.withBuildScript { bs ->
            bs.buildscript = null
            bs.plugins(Plugins.dependencyAnalysis)
          }
        }
        .withAndroidSubproject('app') { app ->
          app.withBuildScript { bs ->
            bs.plugins = [
              new Plugin("com.android.application", agpVersion),
              new Plugin("org.jetbrains.kotlin.android", Plugins.KOTLIN_VERSION),
              Plugins.dependencyAnalysisNoVersion,
            ]
            bs.android = defaultAndroidAppBlock()
          }
          app.manifest = appManifest('com.example.app')
        }
        .write()
    }
  }
}
