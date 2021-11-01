package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

/**
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/513.
 */
final class AndroidMenuProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidMenuProject(String agpVersion) {
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
      root.withFile('local.properties', """\
        sdk.dir=/home/tony/Android/Sdk
      """.stripIndent())
    }
    builder.withAndroidSubproject('consumer') { consumer ->
      consumer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
        bs.dependencies = [
          Dependency.project('implementation', ':producer'),
        ]
      }
      consumer.manifest = AndroidManifest.defaultLib("com.example.consumer")
      consumer.withFile('src/main/res/menu/a_menu.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <menu xmlns:android="http://schemas.android.com/apk/res/android">
          <item
            android:id="@+id/menu_item"
            android:icon="@drawable/drawable_from_other_module"
            android:showAsAction="always" />
        </menu>
      """.stripIndent())
    }
    builder.withAndroidSubproject('producer') { producer ->
      producer.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
      }
      producer.manifest = AndroidManifest.defaultLib("com.example.producer")
      producer.withFile('src/main/res/drawable/drawable_from_other_module.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="48dp"
            android:height="48dp"
            android:viewportWidth="48"
            android:viewportHeight="48">
          <path
            android:pathData="M-0,0h48v48h-48z"
            android:fillColor="#ff0000"/>
        </vector>
      """.stripIndent())
    }

    builder.build().tap {
      writer().write()
    }
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth =
    AdviceHelper.emptyBuildHealthFor(':', ':consumer', ':producer')
}
