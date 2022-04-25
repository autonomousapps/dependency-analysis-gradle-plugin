package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.compAdviceForDependencies
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.AdviceHelper.transitiveDependency
import static com.autonomousapps.kit.Dependency.commonsCollections
import static com.autonomousapps.kit.Dependency.project

final class TestFixturesTestProject extends AbstractProject {

  final GradleProject gradleProject

  TestFixturesTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, Plugin.javaTestFixturesPlugin]
        bs.dependencies = [
          commonsCollections('api'),
          commonsCollections('testFixturesApi')
        ]
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerTestSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('testImplementation', ':proj', 'test-fixtures')
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
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExampleFixture", "com/example/fixtures",
      """\
        package com.example.fixtures;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExampleFixture {
          private HashBag<String> internalBag;
        }
      """.stripIndent(),
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
        }
      """.stripIndent(),
      "test"
    )
  ]

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  // Note: The 'proj-test-fixtures.jar' is considered part of the 'main variant' of ':proj', which is not correct.
  private final Set<Advice> expectedConsumerAdvice = [
    Advice.ofAdd(transitiveDependency(dependency: ':proj'), 'testImplementation'),
  ]

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    // Not yet implemented: missing advice to move the dependency to 'testFixtures' to implementation
    compAdviceForDependencies(':consumer', expectedConsumerAdvice),
    // Not yet implemented: missing advice to move the dependency of 'testFixtures' to testFixturesImplementation
    emptyCompAdviceFor(':proj')
  ]

}
