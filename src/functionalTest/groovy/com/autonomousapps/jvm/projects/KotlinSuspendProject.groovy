package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.project

final class KotlinSuspendProject extends AbstractProject {

  final GradleProject gradleProject

  KotlinSuspendProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalJvmProperties()
    }
    builder.withSubproject('consumer') { consumer ->
      consumer.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          project('implementation', ':producer')
        ]
      }
      consumer.sources = consumerSources
    }
    builder.withSubproject('producer') { producer ->
      producer.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
      }
      producer.sources = producerSources
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final List<Source> consumerSources = [
    new Source(
      SourceType.KOTLIN, 'Consumer.kt', 'com/example/consumer',
      '''\
        package com.example.consumer
        
        import com.example.producer.*
        
        internal class Consumer {
        
          fun doTheThing() {
            complicatedThing(Datum()) {
              getApiResult()
            }
          }
          
          suspend fun getApiResult(): ApiResult<String, String> = TODO()
        }'''.stripIndent()
    )
  ]

  private static final List<Source> producerSources = [
    new Source(
      SourceType.KOTLIN, 'Producer.kt', 'com/example/producer',
      '''\
        package com.example.producer
        
        fun <E : Any> complicatedThing(
          datum: Datum,
          body: suspend () -> ApiResult<*, E>
        ): Unit = TODO()
        
        class Datum
        
        class ApiResult<A, B>'''.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(':consumer', ':producer',)
}
