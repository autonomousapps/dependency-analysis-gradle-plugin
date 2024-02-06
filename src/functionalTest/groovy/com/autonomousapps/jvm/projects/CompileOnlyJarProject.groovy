// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class CompileOnlyJarProject extends AbstractProject {

  final GradleProject gradleProject

  CompileOnlyJarProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""\
          ext {
            libshared = [
              servlet: fileTree("\${project(':external').buildDir}/libs/external.jar"),
            ]
          }""")
        }
      }
    // consumer
      .withSubproject('proj') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            new Dependency('compileOnly', 'libshared.servlet')
          ]
        }
      }
    // producer
      .withSubproject('external') { s ->
        s.sources = [EXTERNAL_PRODUCER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final Source SOURCE_CONSUMER = new Source(SourceType.JAVA, "AppContextListener", "com/example/servlet",
    """\
      package com.example.servlet;

      import javax.servlet.*;

      public class AppContextListener implements ServletContextListener {}""".stripIndent()
  )

  private static final Source EXTERNAL_PRODUCER = new Source(SourceType.JAVA, "ServletContextListener", "javax/servlet",
    """\
      package javax.servlet;

      public interface ServletContextListener {
        default void contextDestroyed() {};
        default void contextInitialized() {};
      }""".stripIndent()
  )

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj'),
    emptyProjectAdviceFor(':external'),
  ]
}
