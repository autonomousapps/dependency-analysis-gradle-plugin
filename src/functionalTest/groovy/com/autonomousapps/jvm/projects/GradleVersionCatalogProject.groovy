package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies

class GradleVersionCatalogProject extends AbstractProject {

  final GradleProject gradleProject

  GradleVersionCatalogProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { rootProject ->
      rootProject.withBuildScript { buildScript ->
        buildScript.dependencies = [
          new Dependency('implementation', 'libs.logback.classic'),
          new Dependency('implementation', 'libs.kotlin.logging')
        ]
      }
      rootProject.sources = [
        new Source(
          SourceType.KOTLIN,
          'Library',
          'com/example',
          """\
            import mu.KotlinLogging
            private val logger = KotlinLogging.logger {}
            
            fun main(args: Array<String>) {
                logger.debug { "Args: \${args}" }
            }
          """.stripIndent()
        )
      ]
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    return projectAdviceForDependencies(
      ':the-project',
      [
        Advice.ofChange(moduleCoordinates('ch.qos.logback:logback-classic:1.2.6'), 'implementation', 'runtimeOnly')
      ] as Set<Advice>
    )
  }
}
