// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class FeatureVariantInConsumerTestProject extends AbstractProject {

  final GradleProject gradleProject

  FeatureVariantInConsumerTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('producer') { s ->
        s.sources = producerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + Plugin.javaTestFixtures
          bs.java = Java.ofFeatures('extra')
          bs.dependencies = [
            project('api', ':producer'),
            project('testFixturesImplementation', ':producer'),
            project('extraImplementation', ':consumer')
          ]
        }
      }
      .write()
  }

  private producerSources = [
    new Source(
      SourceType.JAVA, "Producer", "com/example",
      """\
        package com.example;
        
        public class Producer {
        }""".stripIndent()
    )
  ]

  private consumerSources = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer;
        
        import com.example.Producer;
        
        public class Consumer {
          public Producer p;
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ConsumerFixtures", "com/example/consumer/fixtures",
      """\
        package com.example.consumer.fixtures;
        
        import com.example.Producer;
        import com.example.consumer.Consumer;
        
        public class ConsumerFixtures {
          private Producer p;
          private Consumer c;
        }""".stripIndent()
      , "testFixtures"),
    new Source(
      SourceType.JAVA, "ConsumerExtra", "com/example/consumer/extra",
      """\
        package com.example.consumer.extra;
        
        import com.example.Producer;
        import com.example.consumer.Consumer;
        
        public class ConsumerExtra {
          private Producer p;
          private Consumer c;
        }""".stripIndent()
      , "extra")
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer')
  ]
}
