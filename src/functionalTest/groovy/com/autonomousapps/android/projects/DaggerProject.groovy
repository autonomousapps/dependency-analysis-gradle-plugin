package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.*

final class DaggerProject extends AbstractProject {

  private final String agpVersion
  private final String projectName
  final GradleProject gradleProject

  DaggerProject(String agpVersion, String projectName) {
    this.agpVersion = agpVersion
    this.projectName = projectName
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject(projectName) { s ->
      s.manifest = AndroidManifest.defaultLib('com.example.lib')
      s.sources = sources
      s.withBuildScript { bs ->
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.dependencies = [
          javaxInject('api'),
          dagger('api'),
          daggerCompiler('kapt')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "DaggerUser", "com/example",
      """\
        package com.example
        
        import javax.inject.Inject

        class DaggerUser @Inject constructor(val s: String)
      """.stripIndent()
    )
  ]

  final ComprehensiveAdvice expectedAdvice = emptyCompAdviceFor(":$projectName")
}
