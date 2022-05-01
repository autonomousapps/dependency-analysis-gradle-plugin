package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.Dependency.*

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/553
final class TestDependenciesProject2 extends AbstractProject {

  final GradleProject gradleProject

  TestDependenciesProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('a') { s ->
      s.sources = sourcesA
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          project('implementation', ':b'),
          commonsCollections('testImplementation'),
          junit('testImplementation'),
        ]
      }
    }
    builder.withSubproject('b') { s ->
      s.sources = sourcesB
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [commonsCollections('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sourcesA = [
    new Source(
      SourceType.JAVA, "A", "com/example/a",
      """\
        package com.example.a;
        
        import com.example.b.B;

        public class A {
          // consistent with `implementation project(':b')`
          private B b;
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, "Spec", "com/example/a",
      """\
        package com.example.a;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        import org.junit.Test;
        
        public class Spec {
          @Test
          public void test() {
            // consistent with `testImplementation commonsCollections` 
            Bag<String> bag = new HashBag<>();
          }
        }
      """.stripIndent(),
      'test'
    )
  ]

  private List<Source> sourcesB = [
    new Source(
      SourceType.JAVA, "B", "com/example/b",
      """\
        package com.example.b;

        import org.apache.commons.collections4.Bag;

        public abstract class B {
          // consistent with `api commonsCollections`
          public abstract Bag<String> bagOfStrings();
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':a'),
    emptyProjectAdviceFor(':b'),
  ]
}
