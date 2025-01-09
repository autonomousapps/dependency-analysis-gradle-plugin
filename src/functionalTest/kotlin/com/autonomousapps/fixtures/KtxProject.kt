// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates

/**
 * This project declares a dependency on "androidx.preference:preference-ktx", but it only uses one
 * of its transitive dependencies, "androidx.preference:preference".
 */
class KtxProject(
  private val agpVersion: String,
  private val ignoreKtx: Boolean,
  private val useKtx: Boolean,
) {

  private val sources = if (useKtx) {
    mapOf(
      "BasePreferenceFragment.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      import androidx.preference.PreferenceFragmentCompat

      abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
      }
    """.trimIndent()
    )
  } else {
    mapOf(
      "Lib.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      class Lib {
        fun magic(): Int = 42
      }
    """.trimIndent()
    )
  }

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}",
      "implementation" to "androidx.preference:preference-ktx:1.1.0",
      "implementation" to "androidx.appcompat:appcompat:1.1.0"
    )
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion, extensionSpec = extensionSpec),
    appSpec = appSpec
  )

  private val extensionSpec = """
    dependencyAnalysis {
      structure {
        ignoreKtx($ignoreKtx)
      }
    }
    """.trimIndent()

  val expectedAdviceForApp: Set<Advice>
    get() {
      return if (ignoreKtx) {
        // Ignoring ktx means we will not suggest removing unused -ktx deps IF we also are using a transitive
        if (useKtx) {
          // Since we're using a transitive, we should not suggest removing anything
          emptySet()
        } else {
          // Since we're NOT using a transitive, we should suggest removing the unused dep
          setOf(removeKtx)
        }
      } else {
        // Not ignoring ktx means we will suggest removing -ktx dependencies if they're unused, and adding transitives
        // contributed by the -ktx dependency.
        if (useKtx) {
          // Suggest changing if being used
          setOf(removeKtx, addTransitive)
        } else {
          // Suggest removing unused dependencies
          setOf(removeKtx)
        }
      }
    }

  private val removeKtx = Advice.ofRemove(
    ModuleCoordinates("androidx.preference:preference-ktx", "1.1.0", GradleVariantIdentification.EMPTY),
    "implementation"
  )

  private val addTransitive = Advice.ofAdd(
    ModuleCoordinates("androidx.preference:preference", "1.1.0", GradleVariantIdentification.EMPTY),
    toConfiguration = "implementation"
  )
}
