package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.emptyBuildHealthFor

final class AndroidKotlinInlinePackageCollisionProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidKotlinInlinePackageCollisionProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('lib-consumer') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = [
          Dependency.project('implementation', ":lib-producer")
        ]
      }
      l.manifest = AndroidManifest.defaultLib("com.example.main")
      l.sources = [
        new Source(
          SourceType.KOTLIN, 'Main', 'com/example/main',
          """\
            package com.example.main
            
            import com.example.lib.foo
            
            fun main() = foo()
          """.stripIndent()
        )
      ]
    }
    builder.withSubproject('lib-producer') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
      }
      l.sources = [
        new Source(
          SourceType.KOTLIN, 'Bar', 'com/example/lib',
          """\
            package com.example.lib
            
            inline fun bar(): Int = 1
          """.stripIndent()
        ),
        new Source(
          SourceType.KOTLIN, 'Foo', 'com/example/lib',
          """\
            package com.example.lib
            
            inline fun foo(): Int = 2
          """.stripIndent()
        )
      ]
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = emptyBuildHealthFor(
    ':', ':lib-consumer', ':lib-producer'
  )
}
