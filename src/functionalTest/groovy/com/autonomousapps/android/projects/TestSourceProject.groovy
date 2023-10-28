package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class TestSourceProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  TestSourceProject(String agpVersion) {
    super(agpVersion)
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
      subproject.withBuildScript { bs ->
        bs.plugins = [Plugin.androidApp, Plugin.kotlinAndroid]
        bs.android = androidAppBlock()
        bs.dependencies = [
          kotlinStdLib('implementation'),
          appcompat('implementation'),
          junit('implementation')
        ]
      }
    }
    builder.withAndroidSubproject('lib') { subproject ->
      subproject.sources = androidLibSources
      subproject.manifest = AndroidManifest.defaultLib('my.android.lib')
      subproject.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLib, Plugin.kotlinAndroid]
        bs.android = androidLibBlock(true, 'my.android.lib')
        bs.dependencies = [
          appcompat('implementation'),
          junit('implementation')
        ]
      }
    }
    builder.withSubproject('lib-java') { subproject ->
      subproject.sources = javaLibSources
      subproject.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [junit('implementation')]
      }
    }
    builder.withSubproject('lib-kt') { subproject ->
      subproject.sources = ktLibSources
      subproject.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinNoVersion]
        bs.dependencies = [
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
      'test'
    )
  ]

  private androidLibSources = [
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
      'test'
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
      'test'
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
      'test'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static ProjectAdvice app() {
    projectAdviceForDependencies(':app', changeJunit())
  }

  private static ProjectAdvice libAndroid() {
    projectAdviceForDependencies(':lib', changeJunit())
  }

  private static ProjectAdvice libJava() {
    projectAdviceForDependencies(':lib-java', changeJunit())
  }

  private static ProjectAdvice libKt() {
    projectAdviceForDependencies(':lib-kt', changeJunit())
  }

  private static Set<Advice> changeJunit() {
    return [Advice.ofChange(
      moduleCoordinates('junit:junit', '4.13'), 'implementation', 'testImplementation'
    )]
  }

  Set<ProjectAdvice> expectedBuildHealth = [app(), libAndroid(), libJava(), libKt()]
}
