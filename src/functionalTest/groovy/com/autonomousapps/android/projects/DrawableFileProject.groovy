// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
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
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('consumer') { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = androidAppPlugin
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [project('implementation', ':producer')]
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
      .withAndroidSubproject('producer') { producer ->
        producer.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false)
        }
        producer.manifest = libraryManifest('com.example.producer')
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
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth =
    emptyProjectAdviceFor(':consumer', ':producer')
}
