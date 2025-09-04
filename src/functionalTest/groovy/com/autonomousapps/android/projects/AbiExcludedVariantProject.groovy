// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.PluginAdvice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

final class AbiExcludedVariantProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  AbiExcludedVariantProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.withGroovy("""\
            dependencyAnalysis {
              abi {
                exclusions {
                  excludeVariants('release')
                }
              }
            }"""
          )
        }
      }
      .withAndroidSubproject('lib') { l ->
        l.sources = libSources
        l.manifest = libraryManifest()
        l.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock()
          bs.dependencies = [
            // This dependency would normally cause an unused dependency warning
            // but we're excluding the release variant from analysis
            commonsCollections("releaseImplementation")
          ]
        }
      }
      .write()
  }

  private List<Source> libSources = [
    // Only has code in debug source set - no code in main source set
    new Source(
      SourceType.KOTLIN, "DebugLibrary", "com/example/lib",
      """\
        package com.example.lib
        
        class DebugLibrary
      """.stripIndent(),
      "debug"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final AndroidScore androidScore = androidScoreBuilder().with {
    hasAndroidAssets = false
    hasAndroidRes = false
    usesAndroidClasses = false
    hasBuildConfig = false
    hasAndroidDependencies = false
    hasBuildTypeSourceSplits = true
    build()
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdvice(
      ':lib',
      [] as Set<Advice>,
      [] as Set<PluginAdvice>,
      [androidScore] as Set<ModuleAdvice>,
      false
    ),
  ]
}
