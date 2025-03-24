package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class TestFixturesProject extends AbstractProject {

  final GradleProject gradleProject

  TestFixturesProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withSubproject('consumer') { c ->
        c.sources = consumerSources
        c.withBuildScript { bs ->
          bs.plugins = kotlin + plugins.javaTestFixtures
          bs.dependencies = [
            project('implementation', ':producer'),
            project('api', ':producer').onTestFixtures(),
          ]
        }
      }
      .withSubproject('producer') { c ->
        c.sources = producerSources
        c.withBuildScript { bs ->
          bs.plugins = kotlin + plugins.javaTestFixtures
        }
      }
      .write()
  }

  private consumerSources = [
    Source.kotlin(
      """\
      package com.example.consumer
      
      import com.example.producer.Producer
      
      class Consumer(val producer: Producer)
      """
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
    Source.kotlin(
      """\
      package com.example.consumer
      
      import com.example.producer.FakeProducer
      
      class TestConsumer {
        private fun usesProducerTestFixture() {
          val producer = FakeProducer()
        }
      }
      """
    )
      .withPath('com.example.consumer', 'TestConsumer')
      .build(),
  ]

  private producerSources = [
    Source.kotlin(
      """\
      package com.example.producer
            
      class Producer
      """
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
    Source.kotlin(
      """\
      package com.example.producer
            
      class FakeProducer
      """
    )
      .withPath('com.example.producer', 'FakeProducer')
      .withSourceSet('testFixtures')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'api'),
    Advice.ofChange(
      projectCoordinates(':producer', "the-project:producer${GradleVariantIdentification.TEST_FIXTURES}"),
      'api',
      'implementation'
    ),
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
  ]
}
