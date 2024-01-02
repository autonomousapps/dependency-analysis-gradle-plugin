// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class MultipleJarsProject extends AbstractProject {

  final GradleProject gradleProject

  MultipleJarsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('producer') { s ->
      s.sources = producerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.withGroovy('''
          def extraJar = tasks.register("extraJar", Jar) {
            archiveClassifier = 'extra'
          }
          configurations.apiElements.outgoing {
            // make sure the extraJar (which is empty) turns up first
            artifacts.clear()
            artifact(extraJar)
            artifact(tasks.jar)
          }
        ''')
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          project('implementation', ':producer')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private producerSources = [
    new Source(
      SourceType.JAVA, "ExampleProducer", "com/example/producer",
      """\
        package com.example.producer;
        
        public class ExampleProducer {
        }
      """.stripIndent()
    )
  ]

  private consumerSources = [
    new Source(
      SourceType.JAVA, "ExampleConsumer", "com/example/consumer",
      """\
        package com.example.consumer;
        
        import com.example.producer.ExampleProducer;
        
        public class ExampleConsumer {
          public ExampleProducer producer;
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> producerProjAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'api')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', producerProjAdvice),
    emptyProjectAdviceFor(':producer')
  ]
}
