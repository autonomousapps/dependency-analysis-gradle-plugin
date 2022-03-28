package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*

final class KaptProject extends AbstractProject {

  final String agpVersion
  final GradleProject gradleProject

  KaptProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        bs.additions = """
          dependencyAnalysis {
            issues {
              all {
                onRedundantPlugins {
                  severity('fail')
                  exclude('kotlin-kapt')
                }
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withAndroidSubproject('lib') { a ->
      a.sources = sources
      a.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final List<Source> sources = [
    new Source(
      SourceType.KOTLIN, 'Main', 'com/example',
      """\
        package com.example
        
        class Main
       """.stripIndent()
    )
  ]

  private List<Dependency> dependencies = [
    Dependency.appcompat("implementation"),
    Dependency.dagger("androidTestImplementation"),
    Dependency.daggerCompiler("kaptAndroidTest")
  ]
}
