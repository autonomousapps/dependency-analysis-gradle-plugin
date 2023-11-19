package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib
import static com.autonomousapps.kit.gradle.dependencies.Plugins.KOTLIN_VERSION

final class DoubleDeclarationsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DoubleDeclarationsProject(String agpVersion) {
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
        bs.withGroovy("""\
          subprojects {
            apply plugin: 'com.android.library'
            dependencies {
              implementation 'org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION'
            }
          }""")
      }
    }
    builder.withAndroidSubproject('lib') { a ->
      a.sources = sources
      a.manifest = libraryManifest()
      a.withBuildScript { bs ->
        bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroid]
        bs.android = defaultAndroidLibBlock(true)
        bs.dependencies = dependencies
      }
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
