package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin

final class KaptProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  KaptProject(String agpVersion) {
    super(agpVersion)
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
      a.manifest = libraryManifest()
      a.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLib, Plugin.kotlinAndroid, Plugin.kapt]
        bs.android = androidLibBlock(true)
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
