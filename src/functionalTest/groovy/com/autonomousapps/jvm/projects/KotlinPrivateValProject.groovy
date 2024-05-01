package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

/**
 * https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1172
 */
final class KotlinPrivateValProject extends AbstractProject {

  final GradleProject gradleProject

  KotlinPrivateValProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.gradleProperties += GradleProperties.kotlinStdlibNoDefaultDeps()
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.dependencies(
            project('implementation', ':producer'),
            kotlinStdLib('api'),
          )
        }
      }
      .withSubproject('producer') { s ->
        s.sources = producerSources
        s.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.dependencies(kotlinStdLib('api'))
        }
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.kotlin(
      '''\
      package com.example.consumer
      
      import com.example.producer.ExternalEnum
      
      private val FOO_ENUMS = ExternalEnum.entries.filter { it.name.startsWith("FOO") }
      
      internal class Consumer(private val foos: List<ExternalEnum> = FOO_ENUMS)
      '''.stripMargin()
    )
      .withPath('com.example.consumer', 'Consumer')
      .build()
  ]

  private List<Source> producerSources = [
    Source.kotlin(
      '''\
      package com.example.producer
      
      enum class ExternalEnum {
        FOO_ONE,
        FOO_TWO,
        BAR,
        ;
      }
      '''.stripMargin()
    )
      .withPath('com.example.producer', 'ExternalEnum')
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
