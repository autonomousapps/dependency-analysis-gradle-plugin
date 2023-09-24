package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.clikt

final class BundleKmpProject extends AbstractProject {

  final GradleProject gradleProject

  BundleKmpProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('consumer') { c ->
      c.sources = sourcesConsumer
      c.withBuildScript { bs ->
        bs.plugins = [
          Plugin.kotlinPluginNoVersion,
          Plugin.applicationPlugin
        ]
        bs.dependencies = [
          clikt('implementation')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sourcesConsumer = [
    new Source(
      SourceType.KOTLIN, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer
        
        import com.github.ajalt.clikt.core.CliktCommand
        
        class Consumer : CliktCommand() {
          override fun run() {
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':consumer')
  ]
}
