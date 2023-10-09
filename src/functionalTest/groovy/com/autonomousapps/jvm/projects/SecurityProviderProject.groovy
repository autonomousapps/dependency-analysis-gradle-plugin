package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.conscryptUber
import static com.autonomousapps.kit.gradle.Dependency.okHttp

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

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final appAdvice = [
    Advice.ofChange(moduleCoordinates(conscryptUber), conscryptUber.configuration, 'runtimeOnly'),
  ] as Set<Advice>

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', appAdvice)
  ]
}
