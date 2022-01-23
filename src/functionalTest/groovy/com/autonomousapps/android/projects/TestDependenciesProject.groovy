package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class TestDependenciesProject extends AbstractProject {

  final GradleProject gradleProject

  private final String agpVersion
  private final boolean analyzeTests

  TestDependenciesProject(String agpVersion, boolean analyzeTests) {
    this.agpVersion = agpVersion
    this.analyzeTests = analyzeTests
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.buildscript = new BuildscriptBlock(
          Repository.DEFAULT,
          [androidPlugin(agpVersion)]
        )
      }
    }
    builder.withAndroidSubproject('app') { s ->
      s.sources = sourcesApp
      s.manifest = AndroidManifest.app('my.android.app')
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidAppPlugin]
        bs.dependencies = [
          project('implementation', ':lib'),
          appcompat('implementation'),
          commonsCollections('implementation'),
          junit('testImplementation'),
        ]
      }
    }
    builder.withAndroidSubproject('lib') { s ->
      s.sources = sourcesLib
      s.manifest = AndroidManifest.defaultLib('my.android.lib')
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock()
        bs.dependencies = [
          commonsCollections('api'),
          junit('testImplementation')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sourcesApp = [
    new Source(
      SourceType.JAVA, 'App', 'com/example/app',
      """\
        package com.example.app;
        
        import com.example.lib.Lib;

        public class App {
          // consistent with `implementation project(':lib')`
          private Lib lib;
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'Spec', 'com/example/app',
      """\
        package com.example.app;
        
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

  private List<Source> sourcesLib = [
    new Source(
      SourceType.JAVA, 'Lib', 'com/example/lib',
      """\
        package com.example.lib;

        import org.apache.commons.collections4.Bag;

        public abstract class Lib {
          // consistent with `api commonsCollections`
          public abstract Bag<String> bagOfStrings();
        }
      """.stripIndent()
    )
  ]

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  List<ComprehensiveAdvice> expectedBuildHealth() {
    [
      emptyCompAdviceFor(':'),
      appAdvice(),
      libAdvice()
    ]
  }

  private appAdvice() {
    analyzeTests
      ? compAdviceForDependencies(':app', [changeCommonsCollections] as Set<Advice>)
      : compAdviceForDependencies(':app', [removeCommonsCollections] as Set<Advice>)
  }

  private libAdvice() {
    analyzeTests
      ? compAdviceForDependencies(':lib', [removeJunit] as Set<Advice>)
      : emptyCompAdviceFor(':lib')
  }

  private static Advice removeJunit = Advice.ofRemove(
    dependency(junit('testImplementation'))
  )

  private static Advice removeCommonsCollections = Advice.ofRemove(
    dependency(commonsCollections('implementation'))
  )

  private static Advice changeCommonsCollections = Advice.ofChange(
    dependency(commonsCollections('implementation')),
    'testImplementation'
  )
}
