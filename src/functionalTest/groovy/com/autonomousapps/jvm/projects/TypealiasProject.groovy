package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class TypealiasProject extends AbstractProject {

  final GradleProject gradleProject

  TypealiasProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
    // this project exists to test whether the typealias-dependency needs to be declared as 'api'
      .withSubproject('uber-consumer') { c ->
        c.sources = uberConsumerSources
        c.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
          bs.dependencies = [
            project('implementation', ':consumer'),
          ]
        }
      }
      .withSubproject('consumer') { c ->
        c.sources = consumerSources
        c.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
          bs.dependencies = [
            project('implementation', ':alias'),
            project('api', ':producer')
          ]
        }
      }
      .withSubproject('alias') { c ->
        c.sources = aliasSources
        c.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
          bs.dependencies = [
            project('implementation', ':producer')
          ]
        }
      }
      .withSubproject('producer') { c ->
        c.sources = producerSources
        c.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
        }
      }
      .write()
  }

  private uberConsumerSources = [
    Source.kotlin(
      """\
      package com.example.uberconsumer
      
      import com.example.consumer.Consumer
      
      private class UberConsumer(private val consumer: Consumer)
      """
    )
      .withPath('com.example.uberconsumer', 'UberConsumer')
      .build()
  ]

  private consumerSources = [
    Source.kotlin(
      """\
      package com.example.consumer
      
      import com.example.alias.MyAlias
      
      class Consumer(val myAlias: MyAlias)
      """
    )
      .withPath('com.example.consumer', 'Consumer')
      .build()
  ]

  private aliasSources = [
    Source.kotlin(
      """\
      package com.example.alias
      
      typealias MyAlias = com.example.producer.Producer
      """
    )
      .withPath('com.example.alias', 'MyAlias')
      .build()
  ]

  private producerSources = [
    Source.kotlin(
      """\
      package com.example.producer
      
      class Producer
      """
    )
      .withPath('com.example.producer', 'Producer')
      .build()
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = emptyProjectAdviceFor(
    ':uber-consumer', ':consumer', ':alias', ':producer'
  )
}
