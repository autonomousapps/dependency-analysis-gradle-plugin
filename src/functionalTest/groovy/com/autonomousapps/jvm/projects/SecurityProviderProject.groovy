package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.compAdviceForDependencies
import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.kit.Dependency.conscryptUber
import static com.autonomousapps.kit.Dependency.okHttp

final class SecurityProviderProject extends AbstractProject {

  final GradleProject gradleProject

  SecurityProviderProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> plugins = [Plugin.javaLibraryPlugin]

  private final conscryptUber = conscryptUber("implementation")

  private List<Dependency> dependencies = [
    conscryptUber,
    okHttp("api")
  ]

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
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

  private final appAdvice = [
    Advice.ofChange(dependency(conscryptUber), 'runtimeOnly'),
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    compAdviceForDependencies(':proj', appAdvice)
  ]
}
