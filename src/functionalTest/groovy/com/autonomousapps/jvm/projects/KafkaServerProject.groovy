// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class KafkaServerProject extends AbstractProject {

  final GradleProject gradleProject

  KafkaServerProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy(
            """\
              dependencyAnalysis {
                usage {
                  analysis {
                    checkSuperClasses true
                  }
                }
              }""".stripIndent()
          )
        }
      }
      .withSubproject('lib') { s ->
        s.sources = libSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            implementation('org.apache.kafka:kafka_2.13:4.3.1'),
            // Shouldn't need this. Use it because 'kafka_2.13' has broken metadata
            implementation('org.apache.kafka:kafka-server:4.3.1'),
          )
        }
      }
      .write()
  }

  private static final List<Source> libSources = [
    Source.java(
      '''\
        package com.example.lib;

        import kafka.server.KafkaConfig;
        import java.util.Properties;

        public class Lib {
          private KafkaConfig config = new KafkaConfig(new Properties());
        }
      '''
    ).build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice() {
    return [emptyProjectAdviceFor(':lib')]
  }
}
