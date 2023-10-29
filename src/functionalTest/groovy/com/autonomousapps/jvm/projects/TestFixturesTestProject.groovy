package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

final class TestFixturesTestProject extends AbstractProject {

  private final boolean ignoreSourceTestFixturesSet

  final GradleProject gradleProject

  TestFixturesTestProject(boolean ignoreSourceTestFixturesSet = false) {
    this.ignoreSourceTestFixturesSet = ignoreSourceTestFixturesSet
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    if (ignoreSourceTestFixturesSet) {
      builder.withRootProject { root ->
        root.withBuildScript { bs ->
          bs.withGroovy('''
            dependencyAnalysis {
              issues {
                all {
                  ignoreSourceSet("unknown", "testFixtures")
                  ignoreSourceSet("unknown") // no effect
                }
              }
            }
          ''')
        }
      }
    }
    builder.withSubproject('producer') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary, Plugin.javaTestFixtures]
        bs.dependencies = [
          commonsCollections('api'),
          commonsCollections('testFixturesApi')
        ]
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerTestSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          project('testImplementation', ':producer', 'test-fixtures')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class Example {
          public HashBag<String> bag;
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExampleFixture", "com/example/fixtures",
      """\
        package com.example.fixtures;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExampleFixture {
          private HashBag<String> internalBag;
        }""".stripIndent(),
      "testFixtures"
    )
  ]

  private consumerTestSources = [
    new Source(
      SourceType.JAVA, "ConsumerTest", "com/example/consumer/test",
      """\
        package com.example.consumer.test;
        
        import com.example.fixtures.ExampleFixture;
        
        public class ConsumerTest {
          private ExampleFixture fixture;
        }""".stripIndent(),
      "test"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedProducerAdvice = [
    Advice.ofChange(moduleCoordinates(commonsCollections('')), 'testFixturesApi', 'testFixturesImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth() {
    [
      emptyProjectAdviceFor(':consumer'),
      ignoreSourceTestFixturesSet
        ? emptyProjectAdviceFor(':producer')
        : projectAdviceForDependencies(':producer', expectedProducerAdvice)
    ]
  }
}
