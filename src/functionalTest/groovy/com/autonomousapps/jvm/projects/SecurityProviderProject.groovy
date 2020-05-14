package com.autonomousapps.jvm.projects

import com.autonomousapps.advice.Advice
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.Source
import com.autonomousapps.fixtures.jvm.SourceType

import static com.autonomousapps.fixtures.jvm.Dependency.conscryptUber
import static com.autonomousapps.fixtures.jvm.Dependency.okHttp

class SecurityProviderProject {

  final JvmProject jvmProject

  SecurityProviderProject() {
    this.jvmProject = build()
  }

  private JvmProject build() {
    def builder = new JvmProject.Builder()

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

  final List<Advice> expectedAdvice = SecurityProviderAdvice.expectedAdvice
}
