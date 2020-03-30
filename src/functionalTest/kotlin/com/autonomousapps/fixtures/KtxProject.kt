package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.Dependency

/**
 * This project declares a dependency on "androidx.preference:preference-ktx", but it only uses one
 * of its transitive dependencies, "androidx.preference:preference".
 */
class KtxProject(
  private val agpVersion: String, private val ignoreKtx: Boolean
) {

  val appSpec = AppSpec(
    sources = mapOf("" to """
      import androidx.preference.PreferenceFragmentCompat

      abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
      }
    """.trimIndent()),
    dependencies = listOf(
      "implementation" to "androidx.preference:preference-ktx:1.1.0"
    )
  )

  fun newProject() = AndroidProject(
    agpVersion = agpVersion,
    appSpec = appSpec,
    extensionSpec = extensionSpec
  )

  private val extensionSpec = """
    dependencyAnalysis {
      issues {
        ignoreKtx($ignoreKtx)
      }
    }
    """.trimIndent()

  val expectedAdviceForApp: Set<Advice>
    get() {
      return if (!ignoreKtx) {
        setOf(Advice.remove(Dependency("androidx.preference:preference-ktx", "1.1.0", "implementation")))
      } else {
        emptySet()
      }
    }
}
