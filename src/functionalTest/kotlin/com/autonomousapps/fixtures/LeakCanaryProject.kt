// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates

class LeakCanaryProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "App.kt" to """
    package $DEFAULT_PACKAGE_NAME
    
    import androidx.appcompat.app.AppCompatActivity
    
    class MainActivity : AppCompatActivity() {
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}",
      "implementation" to APPCOMPAT,
      "debugImplementation" to "com.squareup.leakcanary:leakcanary-android:2.2"
    )
  )

  val expectedAdviceForApp = setOf(
    // TODO(tsr): I think it was a bug that we removed this and replaced with everything else
    //  https://github.com/autonomousapps/dependency-analysis-gradle-plugin/pull/1431/files#diff-72ad430e20ec085805596858063ec46eba5381fae9d6d9a57f09eb9691ebf7b8
    // Advice.ofChange(
    //   ModuleCoordinates("com.squareup.leakcanary:leakcanary-android", "2.2", GradleVariantIdentification.EMPTY),
    //   fromConfiguration = "debugImplementation",
    //   toConfiguration = "debugRuntimeOnly"
    // ),
    Advice.ofAdd(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android-core", "2.2", GradleVariantIdentification.EMPTY),
      toConfiguration = "debugImplementation"
    ),
    Advice.ofAdd(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android-core", "2.2", GradleVariantIdentification.EMPTY),
      toConfiguration = "testImplementation"
    ),
    Advice.ofAdd(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android", "2.2", GradleVariantIdentification.EMPTY),
      toConfiguration = "testImplementation"
    ),
    Advice.ofAdd(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android-core", "2.2", GradleVariantIdentification.EMPTY),
      toConfiguration = "androidTestImplementation"
    ),
    Advice.ofAdd(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android", "2.2", GradleVariantIdentification.EMPTY),
      toConfiguration = "androidTestImplementation"
    ),
  )
}
