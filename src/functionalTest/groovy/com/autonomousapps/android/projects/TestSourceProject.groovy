package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.kit.Dependency.*

class TestSourceProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  TestSourceProject(String agpVersion) {
    this.agpVersion = agpVersion
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
    builder.withAndroidSubproject('app') { subproject ->
      subproject.sources = appSources
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin]
        buildScript.dependencies = [
          kotlinStdLib('implementation'),
          appcompat('implementation'),
          junit('implementation')
        ]
      }
    }
    builder.withSubproject('lib-java') { subproject ->
      subproject.sources = javaLibSources
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = [Plugin.javaLibraryPlugin]
        buildScript.dependencies = [junit('implementation')]
      }
    }
    builder.withSubproject('lib-kt') { subproject ->
      subproject.sources = ktLibSources
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = [Plugin.kotlinPluginNoVersion]
        buildScript.dependencies = [
          kotlinStdLib('api'),
          junit('implementation')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private appSources = [
    new Source(
      SourceType.KOTLIN, 'App', 'com/example',
      """\
        package com.example
      
        class App {
          fun magic() = 42
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'Test', 'com/example',
      """\
        package com.example
      
        import org.junit.Assert.assertTrue
        import org.junit.Test
      
        class Test {
          @Test fun test() {
            assertTrue(true)
          }
        }
      """.stripIndent(),
      "test"
    )
  ]

  private javaLibSources = [
    new Source(
      SourceType.JAVA, 'Lib', 'com/example',
      """\
        package com.example;
      
        class Lib {
          int magic() {
            return 42;
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'LibTest', 'com/example',
      """\
        package com.example;
      
        import org.junit.Test;
      
        class LibTest {
          @Test 
          public void test() { 
          }
        }
      """.stripIndent(),
      "test"
    )
  ]

  private ktLibSources = [
    new Source(
      SourceType.KOTLIN, 'Lib', 'com/example',
      """\
        package com.example
      
        class Lib {
          fun magic() = 42
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'LibTest', 'com/example',
      """\
        package com.example
      
        import org.junit.Test
      
        class LibTest {
          @Test fun test() { 
          }
        }
      """.stripIndent(),
      "test"
    )
  ]

  List<ComprehensiveAdvice> expectedBuildHealth() {
    return [emptyRoot(), app(), libJava(), libKt()]
  }

  private static ComprehensiveAdvice emptyRoot() {
    return new ComprehensiveAdvice(':', [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  }

  private static ComprehensiveAdvice app() {
    return new ComprehensiveAdvice(
      ':app', [changeJunit()] as Set<Advice>, [] as Set<PluginAdvice>, false
    )
  }

  private static ComprehensiveAdvice libJava() {
    return new ComprehensiveAdvice(
      ':lib-java', [changeJunit()] as Set<Advice>, [] as Set<PluginAdvice>, false
    )
  }

  private static ComprehensiveAdvice libKt() {
    return new ComprehensiveAdvice(
      ':lib-kt', [changeJunit()] as Set<Advice>, [] as Set<PluginAdvice>, false
    )
  }

  private static Advice changeJunit() {
    return Advice.ofChange(
      new com.autonomousapps.advice.Dependency('junit:junit', '4.13', 'implementation'),
      'testImplementation'
    )
  }
}
