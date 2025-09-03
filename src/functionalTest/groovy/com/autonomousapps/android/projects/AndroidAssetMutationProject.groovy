// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class AndroidAssetMutationProject extends AbstractAndroidProject {

  private static final String ASSET_PATH = 'src/main/assets/some_fancy_asset.txt'

  final GradleProject gradleProject
  private final String agpVersion

  AndroidAssetMutationProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  void deleteAsset() {
    getAsset().delete()
  }

  private File getAsset() {
    return gradleProject.projectDir(':assets').resolve(ASSET_PATH).toFile()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.manifest = libraryManifest('com.example.lib')
        lib.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion)
          bs.android = defaultAndroidLibBlock(true, 'com.example.lib')
          bs.dependencies(
            implementation(':assets'),
            commonsCollections('implementation'),
            kotlinStdLib('api'),
          )
        }
        lib.sources = sources
      }
      .withAndroidLibProject('assets', 'com.example.lib.assets') { assets ->
        assets.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib, Plugins.dependencyAnalysisNoVersion)
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib.assets')
        }
        assets.manifest = libraryManifest('com.example.lib.assets')
        assets.withFile(ASSET_PATH, 'delete me!')
      }
      .write()
  }

  private sources = [
    Source.kotlin(
      '''\
      package com.example
        
      import android.content.Context
      import org.apache.commons.collections4.Bag
      
      class Library {
        fun usesAssets(context: Context) {
          context.getAssets().open("some_fancy_asset.txt")
        }
        
        private fun newBag(): Bag<String> {
          TODO()
        }
      }'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Advice changeAssets = Advice.ofChange(
    projectCoordinates(":assets"),
    "implementation", "runtimeOnly"
  )

  private final Advice removeAssets = Advice.ofRemove(
    projectCoordinates(":assets"),
    "implementation",
  )

  final Set<ProjectAdvice> expectedOriginalBuildHealth = [
    projectAdviceForDependencies(':lib', [changeAssets] as Set<Advice>),
    emptyProjectAdviceFor(':assets'),
  ]

  final Set<ProjectAdvice> expectedDeletionBuildHealth = [
    projectAdviceForDependencies(':lib', [removeAssets] as Set<Advice>),
    emptyProjectAdviceFor(':assets'),
  ]
}
