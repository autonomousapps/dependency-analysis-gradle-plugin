package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.project

final class TestFixturesTestProject2 extends AbstractProject {

  final GradleProject gradleProject

  TestFixturesTestProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('producer') { s ->
      s.sources = producerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, Plugin.javaTestFixturesPlugin]
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('implementation', ':producer'),
          project('testImplementation', ':producer', 'test-fixtures')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private producerSources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        public class Example {
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExampleFixture", "com/example/fixtures",
      """\
        package com.example.fixtures;
        
        import com.example.Example;
        
        public class ExampleFixture {
          private Example internalExample;
        }
      """.stripIndent(),
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
        }
      """.stripIndent()
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
        }
      """.stripIndent(),
      "test"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer')
  ]

}
