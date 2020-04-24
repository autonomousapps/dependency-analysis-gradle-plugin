package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency

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
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.3.72",
      "implementation" to "androidx.preference:preference-ktx:1.1.0"
    )
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion, extensionSpec = extensionSpec),
    appSpec = appSpec
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
          setOf(removeUsedKtx, addTransitive)
        } else {
          // Suggest removing unused dependencies
          setOf(removeKtx)
        }
      }
    }

  private val removeKtx = Advice.remove(ComponentWithTransitives(
    Dependency(
      identifier = "androidx.preference:preference-ktx",
      resolvedVersion = "1.1.0",
      configurationName = "implementation"
    ),
    mutableSetOf()
  ))

  private val removeUsedKtx = Advice.remove(ComponentWithTransitives(
    Dependency(
      identifier = "androidx.preference:preference-ktx",
      resolvedVersion = "1.1.0",
      configurationName = "implementation"
    ),
    mutableSetOf(Dependency("androidx.preference:preference"))
  ))

  private val addTransitive = Advice.add(transitivePreference, toConfiguration = "implementation")
}

private val transitivePreference = TransitiveDependency(
  Dependency(identifier = "androidx.preference:preference", resolvedVersion = "1.1.0"),
  setOf(Dependency("androidx.preference:preference-ktx"))
)
