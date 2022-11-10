package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

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
      // TODO: should invert the defaults to be null rather than have dummy values
      s.styles = null
      s.strings = null
      s.colors = null
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = [
          commonsCollections('api'),
          junit('testImplementation'),
          mockitoKotlin('testImplementation'),
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
      SourceType.KOTLIN, 'Lib', 'com/example/lib',
      """\
        package com.example.lib

        import org.apache.commons.collections4.Bag

        abstract class Lib {
          // consistent with `api commonsCollections`
          abstract fun bagOfStrings(): Bag<String>
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'LibSpec', 'com/example/lib',
      """\
        package com.example.lib
        
        import org.junit.Test
        import org.mockito.kotlin.given
        
        class LibSpec {
          @Test
          fun test() {
            // consistent with `testImplementation mockitoKotlin` 
            given { }
          }
        }
      """.stripIndent(),
      'test'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    [
      appAdvice(),
      libAdvice()
    ]
  }

  private ProjectAdvice appAdvice() {
    analyzeTests
      ? projectAdviceForDependencies(':app', [changeCommonsCollections] as Set<Advice>)
      : projectAdviceForDependencies(':app', [removeCommonsCollections] as Set<Advice>)
  }

  private ProjectAdvice libAdvice() {
    analyzeTests
      ? projectAdviceForDependencies(':lib', [addMockitoCore] as Set<Advice>)
      : emptyProjectAdviceFor(':lib')
  }

  private static Advice addMockitoCore = Advice.ofAdd(
    moduleCoordinates('org.mockito:mockito-core', '4.0.0'), 'testImplementation'
  )

  private static final commonsCollections = commonsCollections('implementation')

  private static Advice removeCommonsCollections = Advice.ofRemove(
    moduleCoordinates(commonsCollections), commonsCollections.configuration
  )

  private static Advice changeCommonsCollections = Advice.ofChange(
    moduleCoordinates(commonsCollections), commonsCollections.configuration, 'testImplementation'
  )
}
