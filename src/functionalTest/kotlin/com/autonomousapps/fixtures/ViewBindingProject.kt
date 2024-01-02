// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.model.Advice

class ViewBindingProject(private val agpVersion: String) {
  val appSpec = AppSpec(
    sources = mapOf(
      "MainActivity.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      import androidx.appcompat.app.AppCompatActivity
      
      class MainActivity : AppCompatActivity() {
      }
    """.trimIndent()
    ),
    dependencies = listOf(
      "implementation" to KOTLIN_STDLIB_ID,
      "implementation" to APPCOMPAT
    ),
    buildAdditions = "android.buildFeatures.viewBinding true"
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  val expectedAdviceForApp = emptySet<Advice>()
}
