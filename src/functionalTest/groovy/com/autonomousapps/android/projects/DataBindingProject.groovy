// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.fixtures.*
import com.autonomousapps.model.Advice
import kotlin.Pair

import static com.autonomousapps.fixtures.Dependencies.APPCOMPAT
import static com.autonomousapps.fixtures.Dependencies.DEPENDENCIES_KOTLIN_STDLIB

final class DataBindingProject {

  DataBindingProject(String agpVersion) {
    this.agpVersion = agpVersion
  }

  AndroidProject newProject() {
    return new AndroidProject(rootSpec, appSpec)
  }

  private final String agpVersion
  final appSpec = new AppSpec(
    AppType.KOTLIN_ANDROID_APP,
    [
      'MainActivity.kt': """\
        import androidx.appcompat.app.AppCompatActivity
                
        class MainActivity : AppCompatActivity() {
        }
      """.stripIndent()
    ],
    [] as Set<AndroidLayout>,
    DEPENDENCIES_KOTLIN_STDLIB + [new Pair<String, String>('implementation', APPCOMPAT)],
    "android.buildFeatures.dataBinding true"
  )

  private final RootSpec rootSpec = new RootSpec(
    null, "", RootSpec.defaultGradleProperties(), agpVersion
  )

  final Set<Advice> expectedAdviceForApp = []
}
