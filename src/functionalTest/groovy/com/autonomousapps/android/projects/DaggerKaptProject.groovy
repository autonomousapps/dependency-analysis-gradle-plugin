// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.model.Advice
import com.autonomousapps.model.PluginAdvice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.kapt
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class DaggerKaptProject extends AbstractAndroidProject {

  private static final DAGGER_COMPILER = kapt('com.google.dagger:dagger-compiler:2.24')
  private static final DAGGER = implementation('com.google.dagger:dagger:2.24')

  final GradleProject gradleProject
  private final String agpVersion
  private final Spec spec

  enum Spec {
    KAPT_SOURCE_METHOD,
    KAPT_SOURCE_CLASS,
    NO_KAPT_SOURCE,
    NO_KAPT_DECLARATIONS
  }

  DaggerKaptProject(String agpVersion, Spec spec) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.spec = spec
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins += rootKapt
        }
      }
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins(androidApp() + kapt())
          bs.android = defaultAndroidAppBlock()
          bs.dependencies(
            kotlinStdLib('implementation'),
          )
          if (spec != Spec.NO_KAPT_DECLARATIONS) {
            bs.dependencies += DAGGER_COMPILER
            bs.dependencies += DAGGER
          }
        }
        if (spec == Spec.KAPT_SOURCE_METHOD || spec == Spec.KAPT_SOURCE_CLASS) {
          app.sources = appSource()
        }
        app.manifest = AndroidManifest.appEmpty()
      }
      .write()
  }

  private final List<Source> appSource() {
    if (spec == Spec.KAPT_SOURCE_METHOD) {
      [
        Source.kotlin(
          '''\
        package mutual.aid
        
        import javax.inject.Inject
        
        class Thing {
          @Inject lateinit var string: String
        }'''.stripIndent()
        ).build()
      ]
    } else if (spec == Spec.KAPT_SOURCE_CLASS) {
      [
        Source.kotlin(
          '''\
        package mutual.aid
        
        import dagger.Module
        import dagger.Provides
        
        @Module object MyModule {
          @Provides @JvmStatic fun provideString(): String {
            return "magic"  
          }
        }'''.stripIndent()
        ).build()
      ]
    } else {
      throw new RuntimeException("Unknown spec '$spec'.")
    }
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<PluginAdvice> redundantKapt() {
    return isLessThanAgp9 ? [PluginAdvice.redundantKapt()] : [PluginAdvice.redundantLegacyKapt()]
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    if (spec == Spec.KAPT_SOURCE_METHOD) {
      [
        projectAdvice(
          ':app',
          [Advice.ofAdd(moduleCoordinates('javax.inject:javax.inject:1'), 'implementation')] as Set<Advice>,
          [] as Set<PluginAdvice>
        )
      ] as Set<ProjectAdvice>
    } else if (spec == Spec.KAPT_SOURCE_CLASS) {
      [emptyProjectAdviceFor(':app')] as Set<ProjectAdvice>
    } else if (spec == Spec.NO_KAPT_SOURCE) {
      [
        projectAdvice(
          ':app',
          [
            Advice.ofRemove(moduleCoordinates(DAGGER_COMPILER), 'kapt'),
            Advice.ofRemove(moduleCoordinates(DAGGER), 'implementation')
          ] as Set<Advice>,
          redundantKapt()
        )
      ] as Set<ProjectAdvice>
    } else {
      [projectAdvice(':app', emptyAdvice(), redundantKapt())] as Set<ProjectAdvice>
    }
  }
}
