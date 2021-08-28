package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.AdviceHelper.transitiveDependency
import static com.autonomousapps.kit.Dependency.*

abstract class AndroidTestDependenciesProject extends AbstractProject {

  protected static final junit = junit('testImplementation')

  protected final String agpVersion
  abstract GradleProject gradleProject

  AndroidTestDependenciesProject(agpVersion) {
    this.agpVersion = agpVersion
  }

  /**
   * TODO which version of AGP is required for the "androidComponents" DSL block?
   */
  static final class Buildable extends AndroidTestDependenciesProject {

    // TODO we need the implementation dependency to workaround a bug in the graphing algo: it crashes when there's only one node
    /** Should be removed */
    private static final commonsIO = commonsIO('implementation')
    /** Should be removed */
    private static final commonsMath = commonsMath('testImplementation')
    /** Should be `testImplementation` */
    private static final commonsCollections = commonsCollections('implementation')

    Buildable(agpVersion) {
      super(agpVersion)
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
          bs.dependencies = [commonsIO, commonsCollections, commonsMath, junit]
          // TODO update to support more versions of AGP
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
  }

  static final class UsedTransitive extends AndroidTestDependenciesProject {

    /** Unused. Brings along Okio, which is used. */
    private static final okHttp = okHttp('testImplementation')

    UsedTransitive(agpVersion) {
      super(agpVersion)
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
          bs.dependencies = [okHttp, junit]

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
        
        import okio.Buffer;
        import org.junit.Test;
        
        public class Spec {
          @Test
          public void test() {
            Buffer buffer = new Buffer();
          }
        }
      """.stripIndent(),
        'test'
      )
    ]

    final List<Advice> expectedAdvice = [
      Advice.ofRemove(dependency(okHttp)),
      Advice.ofAdd(transitiveDependency(
        dependency: dependency(identifier: 'com.squareup.okio:okio'),
        parents: [dependency(identifier: 'com.squareup.okhttp3:okhttp')]
      ), 'testImplementation')
    ]
  }
}
