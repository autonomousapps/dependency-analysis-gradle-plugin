package com.autonomousapps.android.projects

import com.autonomousapps.kit.AndroidColorRes
import com.autonomousapps.kit.AndroidManifest
import com.autonomousapps.kit.BuildscriptBlock
import com.autonomousapps.kit.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.GradleProperties
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.project

/**
 * In this app project, the only reference to the lib project is through a color resource. Does the plugin correctly say
 * that 'lib' is a used dependency?
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
          appcompat('implementation')
        ]
      }
      app.manifest = appManifest('com.example.app')
      app.sources = appSources
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
//      lib.withFile('src/main/res/drawable/ic_pin.xml', """\
//        <?xml version="1.0" encoding="utf-8"?>
//        <vector xmlns:android="http://schemas.android.com/apk/res/android"
//          android:width="36dp"
//          android:height="36dp"
//          android:viewportWidth="36"
//          android:viewportHeight="36"
//          android:contentDescription="@null"
//          >
//
//          <!-- This usage was not detected -->
//          <path
//              android:fillColor="?themeColor"
//              android:pathData="M0.000418269 15C0.0223146 17.9111 0.904212 20.846 2.71627 23.4108L12.9056 37.9142C13.9228 39.362 16.0781 39.3619 17.0952 37.9141L27.2873 23.4053C29.0977 20.8428 29.9786 17.9098 30.0002 15C30 6.71573 23.2843 0 15 0C6.71573 0 0 6.71573 0 15" />
//        </vector>""".stripIndent()
//      )
    }
//    builder.withAndroidSubproject('producer') { producer ->
//      producer.withBuildScript { bs ->
//        bs.plugins = [Plugin.androidLibPlugin]
//        bs.android = androidLibBlock(false, 'com.example.producer')
//        bs.dependencies = [
//          ANDROIDX_ANNOTATION,
//          APPCOMPAT,
//        ]
//      }
//      producer.manifest = AndroidManifest.defaultLib('com.example.producer')
//      // TODO: should invert the defaults to be null rather than have dummy values
//      producer.styles = null
//      producer.strings = null
//      producer.colors = null
//      producer.withFile('src/main/res/values/resources.xml', """\
//        <resources>
//          <attr name="themeColor" format="color" />
//        </resources>""".stripIndent()
//      )
//      producer.withFile('src/main/res/values/themes.xml', """\
//        <?xml version="1.0" encoding="utf-8"?>
//        <resources xmlns:tools="http://schemas.android.com/tools">
//          <style name="Theme.Sample" parent="Theme.AppCompat.DayNight.NoActionBar">
//              <item name="themeColor">@android:color/holo_red_dark</item>
//          </style>
//        </resources>""".stripIndent()
//      )
//    }

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

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(':app', ':lib')
}
