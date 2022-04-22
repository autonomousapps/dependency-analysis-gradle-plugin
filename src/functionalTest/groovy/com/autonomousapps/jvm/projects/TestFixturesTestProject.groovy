package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.commonsCollections

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

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    // Not yet implemented: missing advice to move the dependency of 'testFixtures' to testFixturesImplementation
    emptyCompAdviceFor(':proj')
  ]

}
