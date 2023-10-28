package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.appcompat
import static com.autonomousapps.kit.gradle.Dependency.project

final class DrawableFileProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DrawableFileProject(String agpVersion) {
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
//      root.withFile('local.properties', """\
//        sdk.dir=/Users/trobalik/Library/Android/Sdk
//      """.stripIndent())
    }
    builder.withAndroidSubproject('consumer') { consumer ->
      consumer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidApp]
        bs.android = androidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          project('implementation', ':producer'),
        ]
      }
      // Empty style res and custom manifest to catch the right resource usage. Else, it would find
      // the colorAccent resource.
      consumer.styles = AndroidStyleRes.EMPTY
      consumer.manifest = AndroidManifest.simpleApp()
      consumer.withFile('src/main/res/values/background_drawable.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
          <drawable name="background_logo">@drawable/logo</drawable>
        </resources>""".stripIndent()
      )
    }
    builder.withAndroidSubproject('producer') { producer ->
      producer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLib]
        bs.android = androidLibBlock(false)
      }
      producer.manifest = libraryManifest('com.example.producer')
      // TODO: should invert the defaults to be null rather than have dummy values
      producer.styles = null
      producer.strings = null
      producer.colors = null
      producer.withFile('src/main/res/drawable/logo.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
          <item>
            <shape>
              <solid android:color="#0568ae"/>
            </shape>
          </item>
        </layer-list>""".stripIndent()
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
