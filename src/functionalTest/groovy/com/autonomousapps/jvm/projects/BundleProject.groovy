// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class BundleProject extends AbstractProject {

  final GradleProject gradleProject

  BundleProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""
          dependencyAnalysis {
            structure {
              bundle('facade') {
                primary(':entry-point')
                includeDependency(':used')
              }
            }
          }
        """)
        }
      }
    // consumer -> unused -> entry-point -> used
    // consumer only uses :used.
    // :used and :entry-point are in a bundle
    // plugin should advise to add :entry-point and remove :unused.
    // it should _not_ advise to add :used.
      .withSubproject('consumer') { c ->
        c.sources = sourcesConsumer
        c.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('implementation', ':unused')
          ]
        }
      }
      .withSubproject('unused') { s ->
        s.sources = sourcesUnused
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':entry-point')
          ]
        }
      }
      .withSubproject('entry-point') { s ->
        s.sources = sourcesEntryPoint
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':used')
          ]
        }
      }
      .withSubproject('used') { s ->
        s.sources = sourcesUsed
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example/consumer",
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
      SourceType.JAVA, "Unused", "com/example/unused",
      """\
        package com.example.unused;
        
        import com.example.entry.EntryPoint;
        
        public abstract class Unused {
          public abstract EntryPoint getEntryPoint();
        }
      """.stripIndent()
    )
  ]

  private sourcesEntryPoint = [
    new Source(
      SourceType.JAVA, "EntryPoint", "com/example/entry",
      """\
        package com.example.entry;
        
        import com.example.used.Used;
        
        public abstract class EntryPoint {
          public abstract Used getUsed();
        }
      """.stripIndent()
    )
  ]

  private sourcesUsed = [
    new Source(
      SourceType.JAVA, "Used", "com/example/used",
      """\
        package com.example.used;
        
        public class Used {}
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(projectCoordinates(':entry-point'), 'implementation'),
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation')
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':unused'),
    emptyProjectAdviceFor(':entry-point'),
    emptyProjectAdviceFor(':used')
  ]
}
