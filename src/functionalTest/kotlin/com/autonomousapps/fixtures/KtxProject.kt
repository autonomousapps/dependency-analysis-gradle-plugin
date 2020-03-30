package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.Dependency

/**
 * This project declares a dependency on "androidx.preference:preference-ktx", but it only uses one
 * of its transitive dependencies, "androidx.preference:preference".
 */
class KtxProject(
  private val agpVersion: String, private val ignoreKtx: Boolean, private val useKtx: Boolean
) {

  private val sources = if (useKtx) {
    mapOf("BasePreferenceFragment.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      import androidx.preference.PreferenceFragmentCompat

      abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
      }
    """.trimIndent())
  } else {
    mapOf("Lib.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      class Lib {
        fun magic(): Int = 42
      }
    """.trimIndent())
  }

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.3.70",
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

  private val removeKtx = Advice.remove(
    Dependency(
      identifier = "androidx.preference:preference-ktx",
      resolvedVersion = "1.1.0",
      configurationName = "implementation"
    )
  )

  private val addTransitive = Advice.add(
    dependency = Dependency(identifier = "androidx.preference:preference", resolvedVersion = "1.1.0"),
    toConfiguration = "implementation"
  )
}
