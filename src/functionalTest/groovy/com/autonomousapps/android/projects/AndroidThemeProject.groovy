// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class AndroidThemeProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidThemeProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('consumer') { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = androidAppPlugin
          bs.android = defaultAndroidAppBlock(false, 'com.consumer')
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
          
            <application android:theme="@style/AppTheme.Dot"/>
          </manifest>'''.stripIndent()
        )
      }
      .withAndroidSubproject('producer') { producer ->
        producer.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.producer')
          bs.dependencies = [
            appcompat('implementation'),
          ]
        }
        producer.manifest = AndroidManifest.defaultLib('com.example.producer')
        producer.styles = AndroidStyleRes.of(
          '''\
          <?xml version="1.0" encoding="utf-8"?>
          <resources>
            <style name="AppTheme.Dot" parent="Theme.AppCompat.Light">
              <item name="colorPrimary">#0568ae</item>
            </style>
          </resources>'''.stripIndent()
        )
      }.write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth =
    emptyProjectAdviceFor(':consumer', ':producer')
}
