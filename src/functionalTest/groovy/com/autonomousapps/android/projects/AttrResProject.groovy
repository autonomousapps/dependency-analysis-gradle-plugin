package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

final class AttrResProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  private static final ANDROIDX_ANNOTATION = new Dependency('compileOnly', 'androidx.annotation:annotation:1.1.0')

  AttrResProject(String agpVersion) {
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
//      root.withFile('local.properties', """\
//        sdk.dir=/home/tony/Android/Sdk
//      """.stripIndent())
    }
    builder.withAndroidSubproject('consumer') { consumer ->
      consumer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
        bs.dependencies = [
          Dependency.project('implementation', ':producer'),
          ANDROIDX_ANNOTATION,
        ]
      }
      consumer.manifest = AndroidManifest.defaultLib("com.example.consumer")
      consumer.withFile('src/main/res/drawable/ic_pin.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
          android:width="36dp"
          android:height="36dp"
          android:viewportWidth="36"
          android:viewportHeight="36">
      
          <!-- This usage was not detected -->
          <path
              android:fillColor="?themeColor"
              android:pathData="M0.000418269 15C0.0223146 17.9111 0.904212 20.846 2.71627 23.4108L12.9056 37.9142C13.9228 39.362 16.0781 39.3619 17.0952 37.9141L27.2873 23.4053C29.0977 20.8428 29.9786 17.9098 30.0002 15C30 6.71573 23.2843 0 15 0C6.71573 0 0 6.71573 0 15" />
        </vector>
      """.stripIndent())
    }
    builder.withAndroidSubproject('producer') { producer ->
      producer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
        bs.dependencies = [
          ANDROIDX_ANNOTATION,
        ]
      }
      producer.manifest = AndroidManifest.defaultLib("com.example.producer")
      producer.withFile('src/main/res/values/resources.xml', """\
        <resources>
          <attr name="themeColor" format="color" />
        </resources>
      """.stripIndent())
      producer.withFile('src/main/res/values/themes.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools">
          <style name="Theme.Sample" parent="Theme.AppCompat.DayNight.NoActionBar">
              <item name="themeColor">@android:color/holo_red_dark</item>
          </style>
        </resources>
      """.stripIndent())
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth =
    AdviceHelper.emptyBuildHealthFor(':consumer', ':producer')
}
