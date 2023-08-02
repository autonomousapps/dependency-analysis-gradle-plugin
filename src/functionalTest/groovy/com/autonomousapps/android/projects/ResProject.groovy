package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.project

/**
 * In this app project, the only reference to the lib project is through a color resource. Does the plugin correctly say
 * that 'lib' is a used dependency?
 *
 * The only reference to the lib2 project is through an ID that lib2 provides.
 */
class ResProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ResProject(String agpVersion) {
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
        bs.plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin]
        bs.android = androidAppBlock()
        bs.dependencies = [
          project('implementation', ':lib'),
          project('implementation', ':lib2'),
          appcompat('implementation')
        ]
      }
      app.manifest = appManifest('com.example.app')
      app.sources = appSources
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
    builder.withAndroidLibProject('lib', 'com.example.lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib')
      }
      // TODO: should invert the defaults to be null rather than have dummy values
      lib.strings = null
      lib.styles = null
      lib.colors = AndroidColorRes.DEFAULT
      lib.manifest = libraryManifest('com.example.lib')
    }
    builder.withAndroidLibProject('lib2', 'com.example.lib2') { lib2 ->
      lib2.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.lib2')
      }
      lib2.manifest = AndroidManifest.defaultLib('com.example.lib2')
      // TODO: should invert the defaults to be null rather than have dummy values
      lib2.styles = null
      lib2.strings = null
      lib2.colors = null
      lib2.withFile('src/main/res/values/resources.xml', '''\
        <resources>
          <item name="message_layout" type="id"/>
        </resources>'''.stripIndent()
      )
    }

    def project = builder.build()
    project.writer().write()
    return project
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

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(
    ':app',
    ':lib',
    ':lib2',
  )
}
