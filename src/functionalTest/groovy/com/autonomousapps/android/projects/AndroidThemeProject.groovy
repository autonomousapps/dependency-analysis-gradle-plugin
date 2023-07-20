package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.project

final class AndroidThemeProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidThemeProject(String agpVersion) {
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
    builder.withAndroidSubproject('consumer') { consumer ->
      consumer.withBuildScript { bs ->
        bs.plugins = androidAppPlugin
        bs.android = androidAppBlock(false, 'com.consumer')
        bs.dependencies = [
          project('implementation', ':producer'),
        ]
      }
      consumer.styles = AndroidStyleRes.EMPTY
      consumer.manifest = AndroidManifest.of(
        '''\
          <?xml version="1.0" encoding="utf-8"?>
          <manifest
            xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:tools="http://schemas.android.com/tools"
            package="com.consumer">
          
            <application android:theme="@style/AppTheme"/>
          </manifest>'''.stripIndent()
      )
    }
    builder.withAndroidSubproject('producer') { producer ->
      producer.withBuildScript { bs ->
        bs.plugins = androidLibPlugin
        bs.android = androidLibBlock(false, 'com.example.producer')
        bs.dependencies = [
          appcompat('implementation'),
        ]
      }
      producer.manifest = AndroidManifest.defaultLib('com.example.producer')
      producer.styles = AndroidStyleRes.of(
        '''\
          <?xml version="1.0" encoding="utf-8"?>
          <resources>
            <style name="AppTheme" parent="Theme.AppCompat.Light">
              <item name="colorPrimary">#0568ae</item>
            </style>
          </resources>'''.stripIndent()
      )
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth =
    emptyProjectAdviceFor(':consumer', ':producer')
}
