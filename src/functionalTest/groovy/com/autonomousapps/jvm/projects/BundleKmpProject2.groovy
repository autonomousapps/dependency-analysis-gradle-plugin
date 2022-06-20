package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.okio3
import static com.autonomousapps.kit.Dependency.project

final class BundleKmpProject2 extends AbstractProject {

  private final kotlinLibrary = [Plugin.kotlinPluginNoVersion]
  final GradleProject gradleProject

  BundleKmpProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('consumer') { s ->
      s.sources = sourcesConsumer
      s.withBuildScript { bs ->
        bs.plugins = kotlinLibrary
        bs.dependencies = [
          // gets okio-jvm from this
          project('api', ':producer')
        ]
      }
    }
    builder.withSubproject('producer') { s ->
      s.sources = sourcesProducer
      s.withBuildScript { bs ->
        bs.plugins = kotlinLibrary
        bs.dependencies = [okio3('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sourcesConsumer = [
    new Source(
      SourceType.KOTLIN, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer
        
        import okio.ByteString
        
        interface Consumer {
          fun string(): ByteString
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'ABC', 'com/example/consumer',
      """\
        package com.example.consumer
        
        import com.example.producer.Producer
        
        abstract class ABC : Producer
      """.stripIndent()
    )
  ]

  private sourcesProducer = [
    new Source(
      SourceType.KOTLIN, 'Producer', 'com/example/producer',
      """\
        package com.example.producer
        
        import okio.ByteString
        
        interface Producer {
          fun string(): ByteString
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio:3.0.0'), 'api')
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', projAdvice),
    emptyProjectAdviceFor(':producer')
  ]
}
