package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class TestDependenciesProject extends AbstractProject {

  /** Should be removed */
  private static final commonsIO = commonsIO('implementation')
  /** Should be removed */
  private static final commonsMath = commonsMath('testImplementation')
  /** Should be `testImplementation` */
  private static final commonsCollections = commonsCollections('implementation')

  final GradleProject gradleProject

  TestDependenciesProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [commonsIO, commonsCollections, commonsMath, junit('testImplementation')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;

        public class Main {
          public int magic() {
            return 42;
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "Spec", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        import org.junit.Test;
        
        public class Spec {
          @Test
          public void test() {
            Bag<String> bag = new HashBag<>();
          }
        }
      """.stripIndent(),
      'test'
    )
  ]

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  private final projAdvice = [
    Advice.ofRemove(dependency(commonsMath)),
    Advice.ofRemove(dependency(commonsIO)),
    Advice.ofChange(dependency(commonsCollections), 'testImplementation')
  ] as Set<Advice>

  private final projAdviceWithoutTest = [
    Advice.ofRemove(dependency(commonsCollections)),
    Advice.ofRemove(dependency(commonsIO)),
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    emptyCompAdviceFor(':'),
    compAdviceForDependencies(':proj', projAdvice)
  ]

  final List<ComprehensiveAdvice> expectedBuildHealthWithoutTest = [
    emptyCompAdviceFor(':'),
    compAdviceForDependencies(':proj', projAdviceWithoutTest)
  ]
}
