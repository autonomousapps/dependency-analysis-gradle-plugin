package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

final class CompileOnlyJarProject extends AbstractProject {

  final GradleProject gradleProject

  CompileOnlyJarProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """\
          ext {
            libshared = [
              servlet: fileTree("\${project(':external').buildDir}/libs/external.jar"),
            ]
          }
        """.stripIndent()
      }
    }
    // consumer
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE_CONSUMER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        // TODO need a more typesafe way to express this kind of "raw" dependency. kit.Dependency could be a sealed type
        bs.dependencies = [
          'compileOnly libshared.servlet'
        ]
      }
    }
    // producer
    builder.withSubproject('external') { s ->
      s.sources = [EXTERNAL_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE_CONSUMER = new Source(SourceType.JAVA, "AppContextListener", "com/example/servlet",
    """\
      package com.example.servlet;

      import javax.servlet.*;

      public class AppContextListener implements ServletContextListener {}
    """.stripIndent()
  )

  private static final Source EXTERNAL_PRODUCER = new Source(SourceType.JAVA, "ServletContextListener", "javax/servlet",
    """\
      package javax.servlet;

      public interface ServletContextListener {
        default void contextDestroyed() {};
        default void contextInitialized() {};
      }
    """.stripIndent()
  )

  final List<Advice> expectedAdvice = []
}
