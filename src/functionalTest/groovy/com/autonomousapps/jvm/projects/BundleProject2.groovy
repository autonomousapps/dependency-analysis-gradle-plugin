package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class BundleProject2 extends AbstractProject {

  final GradleProject gradleProject

  BundleProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject {
      it.withBuildScript { bs ->
        bs.additions = """
          dependencyAnalysis {
            structure {
              bundle('facade') {
                includeDependency(':unused')
                includeDependency(':used')
              }
            }
          }
        """.stripIndent()
      }
    }
    // consumer -> unused -> used
    // consumer uses :used.
    // :used and :unused are in a bundle
    // plugin should not advise any changes
    builder.withSubproject('consumer') { c ->
      c.sources = sourcesConsumer
      c.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          project('implementation', ':unused')
        ]
      }

    }
    builder.withSubproject('unused') { s ->
      s.sources = sourcesUnused
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          project('api', ':used')
        ]
      }
    }
    builder.withSubproject('used') { s ->
      s.sources = sourcesUsed
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sourcesConsumer = [
    new Source(
      SourceType.JAVA, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer;
        
        import com.example.used.Used;
        
        public class Consumer {
          private Used used;
        }
      """.stripIndent()
    )
  ]

  private sourcesUnused = [
    new Source(
      SourceType.JAVA, 'Unused', 'com/example/unused',
      """\
        package com.example.unused;
        
        import com.example.used.Used;
        
        public class Unused {
          public Used used;
        }
      """.stripIndent()
    )
  ]

  private sourcesUsed = [
    new Source(
      SourceType.JAVA, 'Used', 'com/example/used',
      """\
        package com.example.used;
        
        public class Used {}
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':unused'),
    emptyProjectAdviceFor(':used')
  ]
}
