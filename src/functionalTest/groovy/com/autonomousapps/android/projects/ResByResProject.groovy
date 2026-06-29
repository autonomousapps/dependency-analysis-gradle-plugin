// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.android.AndroidStringRes
import com.autonomousapps.kit.android.AndroidSubproject
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

/**
 * In this app project, there is a layout file that references a string resource by name (not fully-qualified, because
 * XML). Two separate Android libraries provide this same resource. The project has `android.nonTransitiveRClass`
 * enabled. Is this a problem?
 */
final class ResByResProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ResByResProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins(androidApp())
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies(
            project('implementation', ':res1'),
            project('implementation', ':res2'),
          )
        }
        app.manifest = appEmpty()
        app.layouts(
          AndroidLayout
            .named('string_layout.xml')
            .withContent('''\
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:orientation="vertical"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
            
              <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/common_ok" />
            </LinearLayout>'''.stripIndent())
        )
      }
      .withAndroidLibProject('res1') { lib ->
        configureResLib(lib, 'res1')
      }
      .withAndroidLibProject('res2') { lib ->
        configureResLib(lib, 'res2')
      }
      .write()
  }

  private void configureResLib(AndroidSubproject.Builder lib, String name) {
    lib.withBuildScript { bs ->
      bs.plugins(androidLib(false))
      bs.android = defaultAndroidLibBlock(false, "com.example.$name")
    }
    lib.manifest = libraryManifest("com.example.$name")
    lib.strings = new AndroidStringRes(
      '''\
        <resources>
          <string name="common_ok">OK</string>
        </resources>'''.stripIndent()
    )
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app'),
    emptyProjectAdviceFor(':res1'),
    emptyProjectAdviceFor(':res2'),
  ]
}
