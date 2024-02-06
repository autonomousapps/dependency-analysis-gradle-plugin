// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class AbiProject extends AbstractProject {

  final GradleProject gradleProject

  AbiProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            commonsCollections('api'), // should be implementation
            kotlinStdLib('implementation')
          ]
        }
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, "Example", "com/example",
      """\
        package com.example
        
        import org.apache.commons.collections4.bag.HashBag
        
        internal class Example(private val bag: HashBag<String>)
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final projAdvice2 = [Advice.ofChange(
    moduleCoordinates('org.apache.commons:commons-collections4', '4.4'),
    'api', 'implementation'
  )] as Set<Advice>

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':proj', projAdvice2)
  ]
}
