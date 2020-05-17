package com.autonomousapps.jvm.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.conscryptUber
import static com.autonomousapps.kit.Dependency.okHttp

class SecurityProviderProject {

  final GradleProject gradleProject

  SecurityProviderProject() {
    this.gradleProject = build()
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  private GradleProject build() {
    def builder = new GradleProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]
    def dependencies = [
      conscryptUber("implementation"),
      okHttp("api")
    ]
    def source = new Source(
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

    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }

  final List<Advice> expectedAdvice = []
}
