package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.kotlinStdLib
import static com.autonomousapps.kit.Plugin.KOTLIN_VERSION

final class DoubleDeclarationsProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  DoubleDeclarationsProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        bs.additions = """\
          subprojects {
            apply plugin: 'com.android.library'
            dependencies {
              implementation 'org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION'
            }
          }
        """.stripIndent()
      }
    }
    builder.withAndroidSubproject('lib') { a ->
      a.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = dependencies
      }
      a.sources = sources
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Dependency> dependencies = [
    kotlinStdLib('api'),
    appcompat('implementation'),
  ]

  private sources = [new Source(
    SourceType.KOTLIN, 'Main', 'com/example', """\
      package com.example
      
      // The annotation makes the stdlib part of the ABI
      class Main @JvmOverloads constructor(i: Int = 0, j: Int = 0) {
        fun magic() = 42
      }
    """.stripIndent()
  )]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
