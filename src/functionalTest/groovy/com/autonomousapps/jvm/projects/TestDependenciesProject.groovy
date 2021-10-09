package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.kit.Dependency.*

final class TestDependenciesProject extends AbstractProject {

  // TODO we need the implementation dependency to workaround a bug in the graphing algo: it crashes when there's only one node
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

  final List<Advice> expectedAdvice = [
    Advice.ofRemove(dependency(commonsMath)),
    Advice.ofRemove(dependency(commonsIO)),
    Advice.ofChange(dependency(commonsCollections), 'testImplementation')
  ]

  final List<Advice> expectedAdviceWithoutTest = [
    Advice.ofRemove(dependency(commonsCollections)),
    Advice.ofRemove(dependency(commonsIO)),
  ]
}
