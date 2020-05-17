package com.autonomousapps.jvm.projects

import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.*

import static com.autonomousapps.kit.Dependency.okHttp

class AbiExclusionsProject {

  final GradleProject gradleProject

  AbiExclusionsProject() {
    this.gradleProject = build()
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  private GradleProject build() {
    def builder = new GradleProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]

    builder.rootAdditions = """\
      dependencyAnalysis {
        abi {
          exclusions {
            excludeClasses("com\\\\.example\\\\.Main")
          }
        }
      }
    """.stripIndent()

    def dependencies = [
      okHttp("implementation")
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

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdvice(gradleProject)
  }

  final List<Advice> expectedAdvice = []
}
