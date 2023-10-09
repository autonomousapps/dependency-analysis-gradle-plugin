package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class AndroidTestSourceProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final Boolean withKapt

  AndroidTestSourceProject(String agpVersion, Boolean withKapt = false) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.withKapt = withKapt
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
      subproject.sources = appSources()
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = appPlugins()
        buildScript.android = androidAppBlock()
        buildScript.dependencies = appDependencies()
      }
    }
    builder.withAndroidSubproject('lib') { subproject ->
      subproject.sources = androidLibSources
      subproject.manifest = AndroidManifest.defaultLib('my.android.lib')
      subproject.withBuildScript { buildScript ->
        buildScript.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin]
        buildScript.android = androidLibBlock(true, 'my.android.lib')
        buildScript.dependencies = [
          appcompat('implementation'),
          junit('implementation'),
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> appPlugins() {
    def plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin]
    if (withKapt) {
      plugins += Plugin.kaptPlugin
    }
    plugins
  }

  private List<Dependency> appDependencies() {
    def deps = [
      kotlinStdLib('implementation'),
      appcompat('implementation'),
      junit('implementation')
    ]
    if (withKapt) {
      deps.addAll([daggerCompiler('kaptAndroidTest'), dagger('androidTestImplementation')])
    }
    deps
  }

  private List<Source> appSources() {
    def sources = [
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
        'androidTest'
      )
    ]

    if (withKapt) {
      sources += new Source(
        SourceType.KOTLIN, 'TestModule', 'com/example',
        """\
          package com.example
          
          import dagger.Module
          import dagger.Provides
          
          @Module
          class TestModule {
          
            @Provides
            fun provideSampleDep(): String {
              return "Hello, world!"
            }
          }
        """.stripIndent(),
        'androidTest'
      )
    }
    sources
  }

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
      'androidTest'
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

  private static Set<Advice> changeJunit() {
    return [Advice.ofChange(
      moduleCoordinates('junit:junit', '4.13'), 'implementation', 'androidTestImplementation'
    )]
  }

  Set<ProjectAdvice> expectedBuildHealth = [app(), libAndroid()]
}
