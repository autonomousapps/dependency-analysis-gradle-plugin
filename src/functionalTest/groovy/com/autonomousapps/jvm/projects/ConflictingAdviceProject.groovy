// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.*

final class ConflictingAdviceProject extends AbstractProject {

  final GradleProject gradleProject

  ConflictingAdviceProject() {
    super(getLaterKotlinVersion())
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib') { l ->
        l.sources = libSources
        l.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies(
            implementation(':other-lib'),
            testImplementation('com.squareup.okio:okio:3.16.0'),
          )
        }
      }
      .withSubproject('other-lib') { l ->
        l.sources = otherLibSources
        l.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies(
            // It's important for the repro that this version be different
            api('com.squareup.okio:okio:3.9.1'),
          )
        }
      }
      .write()
  }

  private libSources = [
    Source.kotlin(
      '''\
        package com.example.lib
        
        import com.example.other.lib.OtherLib
        
        class Lib {
          private val otherLib = OtherLib()
        }
      '''.stripIndent()
    ).build(),
    Source.kotlin(
      '''\
        package com.example.lib.test
        
        import okio.Buffer
        
        class Test {
          private val buffer = Buffer()
        }
      '''.stripIndent()
    )
      .withSourceSet('test')
      .build(),
  ]

  private otherLibSources = [
    Source.kotlin(
      '''\
        package com.example.other.lib
        
        import okio.Buffer
        
        class OtherLib {
          val buffer = Buffer()
        }
      '''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':lib'),
    emptyProjectAdviceFor(':other-lib'),
  ]
}
