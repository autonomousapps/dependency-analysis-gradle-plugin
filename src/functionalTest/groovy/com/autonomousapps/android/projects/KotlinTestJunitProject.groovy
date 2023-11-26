package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class KotlinTestJunitProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  KotlinTestJunitProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { subproject ->
        subproject.sources = appSources
        subproject.styles = AndroidStyleRes.DEFAULT
        subproject.colors = AndroidColorRes.DEFAULT
        subproject.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp, Plugins.kotlinAndroid]
          bs.android = defaultAndroidAppBlock()
          bs.dependencies = [
            kotlinTestJunit('androidTestImplementation'),
            junit('androidTestImplementation'),
            appcompat('implementation'),
          ]
        }
      }
      .write()
  }

  private appSources = [
    new Source(
      SourceType.KOTLIN, 'App', 'com/example',
      """\
        package com.example
      
        class App {
          fun magic() = 42
        }""".stripIndent()
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
        }""".stripIndent(),
      'androidTest'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static ProjectAdvice app() {
    projectAdviceForDependencies(':app', changeKotlinTestJunit())
  }

  private static Set<Advice> changeKotlinTestJunit() {
    return [Advice.ofChange(
      moduleCoordinates('org.jetbrains.kotlin:kotlin-test-junit', Plugins.KOTLIN_VERSION),
      'androidTestImplementation',
      'androidTestRuntimeOnly'
    )]
  }

  Set<ProjectAdvice> expectedBuildHealth = [app()]
}
