// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

/**
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/513.
 */
final class AndroidMenuProject extends AbstractAndroidProject {

  private static final APPCOMPAT = appcompat('implementation')

  final GradleProject gradleProject
  private final String agpVersion

  AndroidMenuProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('consumer') { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(false, 'com.example.consumer')
          bs.dependencies = [project('implementation', ':producer')]
        }
        consumer.manifest = AndroidManifest.defaultLib('com.example.consumer')
        consumer.withFile('src/main/res/menu/a_menu.xml', """\
        <?xml version="1.0" encoding="utf-8"?>
        <menu xmlns:android="http://schemas.android.com/apk/res/android">
          <item
            android:id="@+id/menu_item"
            android:icon="@drawable/drawable_from_other_module"
            android:showAsAction="always" />
        </menu>""".stripIndent()
        )
      }
      .withAndroidSubproject('producer') { producer ->
        producer.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(false, 'com.example.producer')
        }
        producer.manifest = AndroidManifest.defaultLib('com.example.producer')
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
        </vector>""".stripIndent()
        )
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth =
    emptyProjectAdviceFor(':consumer', ':producer')
}
