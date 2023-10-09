package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class MixedSourceProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  MixedSourceProject(String agpVersion) {
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
      }
    }
    builder.withAndroidLibProject('consumer', 'com.example.consumer') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = androidLibBlock(false, 'com.example.consumer')
        bs.dependencies = [
          project('implementation', ':lib'),
        ]
      }
      lib.sources = consumerSources
    }
    builder.withSubproject('lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
      }
      lib.sources = libSources
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private consumerSources = [
    new Source(
      SourceType.JAVA, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer;
        
        import com.example.lib.Lib;
        
        public class Consumer {
                  
          private void magic() {
            new Lib();
          }
        }
      """.stripIndent()
    )
  ]

  private libSources = [
    new Source(
      SourceType.JAVA, 'Lib', 'com/example/lib',
      """\
        package com.example.lib;
        
        // used by :app
        public class Lib {}
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'LibK', 'com/example/lib',
      """\
        package com.example.lib
        
        // unused by :app
        class LibK
      """.stripIndent(),
      Source.DEFAULT_SOURCE_SET,
      'java'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':lib'),
  ]
}
