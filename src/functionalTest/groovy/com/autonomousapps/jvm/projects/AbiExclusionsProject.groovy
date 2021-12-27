package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.okHttp

final class AbiExclusionsProject extends AbstractProject {

  private final okhttp = okHttp('api')
  final GradleProject gradleProject

  AbiExclusionsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """\
          dependencyAnalysis {
            abi {
              exclusions {
                excludeClasses("com\\\\.example\\\\.Main")
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [okhttp]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  def sources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        import okhttp3.OkHttpClient;
        
        public class Main {
          public OkHttpClient ok() {
            return new OkHttpClient.Builder().build();
          }
        }
      """.stripIndent()
    )
  ]

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  private final projAdvice = [
    Advice.ofChange(dependency(okhttp), 'implementation')
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    emptyCompAdviceFor(':'),
    compAdviceForDependencies(':proj', projAdvice)
  ]
}
