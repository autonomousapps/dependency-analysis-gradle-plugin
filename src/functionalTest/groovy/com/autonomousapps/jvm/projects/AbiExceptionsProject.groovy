package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.project

final class AbiExceptionsProject extends AbstractProject {

  final GradleProject gradleProject

  AbiExceptionsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = libSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [project('api', ':exceptions')]
      }
    }
    builder.withSubproject('exceptions') { s ->
      s.sources = exceptionsSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private libSources = [
    new Source(
      SourceType.JAVA, "Sup", "com/example",
      """\
        package com.example;
        
        public interface Sup {}""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import com.example.exception.FancyException;
        
        public class Main implements Sup {
          public String magic() throws FancyException {
            return "42";
          }
        }
      """.stripIndent()
    )
  ]

  private exceptionsSources = [
    new Source(
      SourceType.JAVA, "FancyException", "com/example/exception",
      """\
        package com.example.exception;
                
        public class FancyException extends RuntimeException {}
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = emptyProjectAdviceFor(
    ':proj', ':exceptions'
  )
}
