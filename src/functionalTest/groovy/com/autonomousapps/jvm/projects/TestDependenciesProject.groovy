package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

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
        bs.plugins = [Plugin.javaLibrary]
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

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    Advice.ofRemove(moduleCoordinates(commonsMath), commonsMath.configuration),
    Advice.ofRemove(moduleCoordinates(commonsIO), commonsIO.configuration),
    Advice.ofChange(moduleCoordinates(commonsCollections), commonsCollections.configuration, 'testImplementation')
  ]

  private final Set<Advice> projAdviceWithoutTest = [
    Advice.ofRemove(moduleCoordinates(commonsCollections), commonsCollections.configuration),
    Advice.ofRemove(moduleCoordinates(commonsIO), commonsIO.configuration),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', projAdvice)
  ]

  final Set<ProjectAdvice> expectedBuildHealthWithoutTest = [
    projectAdviceForDependencies(':proj', projAdviceWithoutTest)
  ]
}
