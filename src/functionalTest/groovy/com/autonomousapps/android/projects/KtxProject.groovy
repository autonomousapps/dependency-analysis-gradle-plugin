// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

/**
 * This project declares a dependency on "androidx.preference:preference-ktx", but it only uses one of its transitive
 * dependencies, "androidx.preference:preference".
 */
final class KtxProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final boolean ignoreKtx
  private final boolean useKtx

  KtxProject(String agpVersion, boolean ignoreKtx, boolean useKtx) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.ignoreKtx = ignoreKtx
    this.useKtx = useKtx
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy(
            """\
              dependencyAnalysis {
                structure {
                  ignoreKtx($ignoreKtx)
                }
              }
            """.stripIndent()
          )
        }
      }
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidApp()
          bs.android = defaultAndroidAppBlock(true, 'com.example.app')
          bs.dependencies(
            kotlinStdLib('implementation'),
            appcompat('implementation'),
            implementation('androidx.preference:preference-ktx:1.1.0'),
          )
        }
        app.sources = appSource()
        app.manifest = AndroidManifest.app()
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
      }
      .write()
  }

  private List<Source> appSource() {
    if (useKtx) {
      return [
        Source.kotlin(
          '''\
            package mutual.aid
            
            import androidx.preference.PreferenceFragmentCompat
                        
            abstract class BasePreferenceFragment : PreferenceFragmentCompat() {}'''.stripIndent()
        ).build()
      ]
    } else {
      return [
        Source.kotlin(
          '''\
            package mutual.aid
            
            class Lib {
              fun magic(): Int = 42
            }'''.stripIndent()
        ).build()
      ]
    }
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private Advice removeKtx = Advice.ofRemove(
    moduleCoordinates('androidx.preference:preference-ktx:1.1.0'),
    'implementation',
  )
  private Advice addAndroidxPreference = Advice.ofAdd(
    moduleCoordinates('androidx.preference:preference:1.1.0'),
    'implementation',
  )

  private Set<Advice> appAdvice() {
    Set<Advice> advice
    if (ignoreKtx) {
      // Ignoring ktx means we will not suggest removing unused -ktx deps IF we also are using a transitive
      if (useKtx) {
        // Since we're using a transitive, we should not suggest removing anything
        advice = []
      } else {
        // Since we're NOT using a transitive, we should suggest removing the unused dep
        advice = [removeKtx]
      }
    } else {
      // Not ignoring ktx means we will suggest removing -ktx dependencies if they're unused, and adding transitives
      // contributed by the -ktx dependency.
      if (useKtx) {
        // Suggest changing if being used
        advice = [removeKtx, addAndroidxPreference]
      } else {
        // Suggest removing unused dependencies
        advice = [removeKtx]
      }
    }

    return advice
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdviceForDependencies(':app', appAdvice())
    ]
  }
}
