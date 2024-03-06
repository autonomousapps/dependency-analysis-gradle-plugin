// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class TestFixturesTestProject2 extends AbstractProject {

  final GradleProject gradleProject
  private final String producerProjectPath

  TestFixturesTestProject2(boolean withNestedProjects) {
    this.producerProjectPath = withNestedProjects ? ':nested:producer' : ':producer'
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject(producerProjectPath) { s ->
        s.sources = producerSources
        s.withBuildScript { bs ->
          bs.withGroovy('group = "org.example.producer"')
          bs.plugins = [Plugin.javaLibrary, Plugin.javaTestFixtures, Plugins.dependencyAnalysisNoVersion]
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', producerProjectPath),
            project('api', producerProjectPath, 'test-fixtures'),
            project('testImplementation', producerProjectPath, 'test-fixtures')
          ]
        }
      }
      .write()
  }

  private producerSources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        public class Example {
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExampleFixture", "com/example/fixtures",
      """\
        package com.example.fixtures;
        
        import com.example.Example;
        
        public class ExampleFixture {
          private Example internalExample;
        }""".stripIndent(),
      "testFixtures"
    )
  ]

  private consumerSources = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example",
      """\
        package com.example.consumer;
        
        import com.example.Example;
        
        public class Consumer {
          private Example internalExample;
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ConsumerTest", "com/example/consumer/test",
      """\
        package com.example.consumer.test;
        
        import com.example.consumer.Consumer;
        import com.example.fixtures.ExampleFixture;
        
        public class ConsumerTest {
          private ExampleFixture fixture;
          private Consumer consumer;
        }""".stripIndent(),
      "test"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedConsumerAdvice() {
    [
      Advice.ofChange(projectCoordinates(producerProjectPath), 'api', 'implementation'),
      Advice.ofRemove(projectCoordinates(producerProjectPath, 'org.example.producer:producer-test-fixtures'), 'api')
    ]
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    [
      projectAdviceForDependencies(':consumer', expectedConsumerAdvice()),
      emptyProjectAdviceFor(producerProjectPath)
    ]
  }
}
