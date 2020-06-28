package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.kotlinStdLib

final class RedundantPluginsProject extends AbstractProject {

  final GradleProject gradleProject

  RedundantPluginsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """\
          dependencyAnalysis {
            issues {
              all {
                onRedundantPlugins {
                  severity 'fail'
                }
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('implementation')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  def sources = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        class Main {
          fun magic() = 42
        }
      """.stripIndent()
    )
  ]

  List<BuildHealth> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<BuildHealth> expectedBuildHealth = [
    new BuildHealth(
      ':proj',
      [] as Set<Advice>,
      [PluginAdvice.redundantJavaLibrary()] as Set<PluginAdvice>,
      true
    ),
    new BuildHealth(':', [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  ]
}
