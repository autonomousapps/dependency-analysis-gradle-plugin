// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice

class AppCompatProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "Lib.kt" to """
    package $DEFAULT_PACKAGE_NAME
     
    class Lib {
      fun magic() = 42
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}",
      "implementation" to APPCOMPAT
    )
  )

  val expectedAdviceForApp = emptySet<Advice>()
}
