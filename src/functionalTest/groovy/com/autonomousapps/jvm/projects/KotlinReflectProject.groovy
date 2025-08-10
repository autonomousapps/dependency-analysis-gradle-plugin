// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinReflect

final class KotlinReflectProject extends AbstractProject {

  final GradleProject gradleProject

  KotlinReflectProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            // Must not be advised to move to runtimeOnly
            kotlinReflect('implementation')
          ]
        }
      }
      .write()
  }

  private sources = [
    Source.kotlin(
      '''\
        package com.example
        
        sealed class SealedClass {
          class SubClass : SealedClass() {
            fun usesKotlinReflect(): String {
              return SealedClass::class.sealedSubclasses.joinToString()
            }
          }
        }
      '''
    )
      .withPath('com.example', 'SealedClass')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj')
  ]
}
