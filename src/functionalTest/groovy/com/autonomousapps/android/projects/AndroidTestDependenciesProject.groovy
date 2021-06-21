package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.kit.Dependency.*

/**
 * TODO which version of AGP is required for the "androidComponents" DSL block?
 */
final class AndroidTestDependenciesProject extends AbstractProject {

  // TODO we need the implementation dependency to workaround a bug in the graphing algo: it crashes when there's only one node
  /** Should be removed */
  private static final commonsIO = commonsIO('implementation')
  /** Should be removed */
  private static final commonsMath = commonsMath('testImplementation')
  /** Should be `testImplementation` */
  private static final commonsCollections = commonsCollections('implementation')

  private final String agpVersion
  final GradleProject gradleProject

  AndroidTestDependenciesProject(agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(false)
        bs.dependencies = [commonsIO, commonsCollections, commonsMath, junit('testImplementation')]
        bs.additions = """\
          androidComponents {
            beforeUnitTests(selector().withBuildType("release")) {
              enabled = false
            }
          }
        """.stripIndent()
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
}
