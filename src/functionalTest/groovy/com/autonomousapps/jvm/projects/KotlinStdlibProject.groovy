package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.kotlinStdlibJdk7

final class KotlinStdlibProject extends AbstractProject {

  final GradleProject gradleProject
  private final String additions

  KotlinStdlibProject(String additions = '') {
    this.additions = additions
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.withBuildScript { buildScript ->
        buildScript.additions = additions
      }
    }
    builder.withSubproject('proj') { subproject ->
      subproject.sources = sources
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = [Plugin.kotlinPlugin(null, true)]
        buildScript.dependencies = [kotlinStdlibJdk7('implementation')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example',
      """\
        package com.example
      
        class Library {
          fun magic() = 42
        }
      """
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBundleBuildHealth = [
    emptyProjectAdviceFor(':proj'),
  ]
}
