// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class AbiExcludedSourceSetProject extends AbstractProject {

  final GradleProject gradleProject

  AbiExcludedSourceSetProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""\
            dependencyAnalysis {
              abi {
                exclusions {
                  excludeSourceSets('functionalTest')
                }
              }
            }"""
          )
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.sourceSets('functionalTest')
          bs.dependencies(project('functionalTestImplementation', ':producer'))
        }
      }
      .withSubproject('producer') { s ->
        s.sources = sourcesProducer
        s.withBuildScript { bs ->
          bs.plugins(kotlin)
        }
      }
      .write()
  }

  private sourcesConsumer = [
    Source.kotlin(
      """\
        package com.example.consumer
        
        import com.example.producer.Producer
        
        class Consumer {
          val producer: Producer = Producer()
        }
      """.stripIndent()
    )
      .withSourceSet('functionalTest')
      .withPath('com.example.consumer', 'Consumer')
      .build()
  ]

  private sourcesProducer = [
    Source.kotlin(
      """\
        package com.example.producer
        
        class Producer
      """.stripIndent()
    )
      .withPath('com.example.producer', 'Producer')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer'),
  ]
}
