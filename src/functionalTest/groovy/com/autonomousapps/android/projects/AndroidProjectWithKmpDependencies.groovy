// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.kotlin.Kotlin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidProjectWithKmpDependencies extends AbstractAndroidProject {

  private static final Dependency KOTLINX_IMMUTABLE = kotlinxImmutable('implementation', "-jvm")
  private static final Dependency KOTLINX_COROUTINES_TEST = kotlinxCoroutinesTest('implementation', "-jvm")
  private static final Dependency COMPOSE_MULTIPLATFORM_FOUNDATION = composeMultiplatformFoundation('implementation')

  final GradleProject gradleProject
  private final String agpVersion
  private final String additions

  AndroidProjectWithKmpDependencies(String agpVersion, String additions = '') {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.additions = additions
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.additions = additions
        }
      }
      .withAndroidSubproject('app') { s ->
        s.manifest = AndroidManifest.app('com.example.MainApplication')
        s.sources = sources
        s.styles = AndroidStyleRes.DEFAULT
        s.colors = AndroidColorRes.DEFAULT
        s.withBuildScript { bs ->
          bs.plugins(androidApp())
          bs.android = defaultAndroidAppBlock(true)
          bs.dependencies = [
            kotlinStdLib('implementation'),
            appcompat('implementation'),

            // Immutable collections JVM dep that should be corrected to the canonical target
            KOTLINX_IMMUTABLE,

            // Coroutines Test JVM dep that should be
            // - Swapped with the core dep
            // - Core dep should use the canonical target
            KOTLINX_COROUTINES_TEST,

            // A foundation compose dependency but we only use runtime APIs
            // This is an odd one because it will actually result in adding
            // the androidx compose dep. See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/919#issuecomment-1620643857
            COMPOSE_MULTIPLATFORM_FOUNDATION,
          ]

          bs.kotlin = Kotlin.DEFAULT
        }
      }
      .write()
  }

  private List<Source> sources = [
    Source.kotlin(
      '''\
        package com.example
        
        import android.app.Application
        import androidx.compose.runtime.Recomposer
        import kotlinx.coroutines.Dispatchers
        import kotlinx.collections.immutable.persistentListOf
      
        class MainApplication : Application() {
          override fun onCreate() {
            val list = persistentListOf(1)
            val recomposer = Recomposer(Dispatchers.IO)
          }
        }'''.stripIndent()
    ).build(),
  ]

  @SuppressWarnings("GrMethodMayBeStatic")
  private Set<Advice> expectedAdvice() {
    return [
      removeComposeFoundation(),
      addComposeRuntime(),
      addKotlinxCoroutinesCore(),
      changeKotlinxCoroutinesTest(),
      // TODO need to make a new advice to replace KMP targets with canonical ones
      //  See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/919#issuecomment-1620684615
      //      removeKotlinxImmutableJvm(),
      //      addKotlinxImmutable(),
    ] as Set<Advice>
  }

  private static Advice removeComposeFoundation() {
    return Advice.ofRemove(moduleCoordinates(COMPOSE_MULTIPLATFORM_FOUNDATION), 'implementation')
  }

  private static Advice addComposeRuntime() {
    return Advice.ofAdd(moduleCoordinates('androidx.compose.runtime:runtime', '1.1.0-beta04'), 'implementation')
  }

  private static Advice addKotlinxCoroutinesCore() {
    return Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-core', '1.7.3'), 'implementation')
  }

  // In practice we don't actually want to keep this, but because it has a serviceloader DAGP will
  // conservatively keep it as runtimeOnly instead of removing
  private static Advice changeKotlinxCoroutinesTest() {
    return Advice.ofChange(
      moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm', '1.7.3'),
      'implementation',
      'runtimeOnly'
    )
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', expectedAdvice()),
  ]
}
