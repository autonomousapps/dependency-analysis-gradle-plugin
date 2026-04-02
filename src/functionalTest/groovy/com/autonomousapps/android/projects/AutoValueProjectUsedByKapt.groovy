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
import static com.autonomousapps.kit.gradle.Dependency.compileOnly
import static com.autonomousapps.kit.gradle.Dependency.kapt
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib
import static com.autonomousapps.model.Advice.ofRemove

final class AutoValueProjectUsedByKapt extends AbstractAndroidProject {

  private static final KAPT = kapt('com.google.auto.value:auto-value:1.7')

  final GradleProject gradleProject
  private final String agpVersion
  private final Spec spec

  enum Spec {
    KAPT_SOURCE,
    NO_KAPT_SOURCE,
    NO_KAPT_DECLARATIONS
  }

  AutoValueProjectUsedByKapt(String agpVersion, Spec spec) {
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
          bs.dependencies(kotlinStdLib('implementation'))
          if (spec != Spec.NO_KAPT_DECLARATIONS) {
            bs.dependencies += KAPT
            bs.dependencies += compileOnly('com.google.auto.value:auto-value-annotations:1.7')
          }
        }
        if (spec == Spec.KAPT_SOURCE) {
          app.sources = appSource
        }
        app.manifest = AndroidManifest.appEmpty()
      }
      .write()
  }

  private final List<Source> appSource = [
    Source.kotlin(
      '''\
        package mutual.aid
        
        import com.google.auto.value.AutoValue
        
        @AutoValue
        abstract class Animal {
          companion object {
            @JvmStatic fun create(name: String, numberOfLegs: Int): Animal = AutoValue_Animal(name, numberOfLegs)
          }
          
          abstract fun name(): String
          abstract fun numberOfLegs(): Int
        }'''.stripIndent()
    ).build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<PluginAdvice> redundantKapt() {
    return isLessThanAgp9 ? [PluginAdvice.redundantKapt()] : [PluginAdvice.redundantLegacyKapt()]
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    if (spec == Spec.KAPT_SOURCE) {
      [emptyProjectAdviceFor(':app')] as Set<ProjectAdvice>
    } else if (spec == Spec.NO_KAPT_SOURCE) {
      [
        projectAdvice(
          ':app',
          [ofRemove(moduleCoordinates(KAPT), 'kapt')] as Set<Advice>,
          redundantKapt()
        )
      ] as Set<ProjectAdvice>
    } else {
      [projectAdvice(':app', emptyAdvice(), redundantKapt())] as Set<ProjectAdvice>
    }
  }
}
