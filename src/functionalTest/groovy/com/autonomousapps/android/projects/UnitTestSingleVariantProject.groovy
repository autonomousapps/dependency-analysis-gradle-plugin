// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.android.AndroidComponents
import com.autonomousapps.kit.gradle.kotlin.Kotlin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

final class UnitTestSingleVariantProject extends AbstractAndroidProject {

  private static final JVM_LIB = implementation(':jvmlib')
  private static final OKHTTP = api('com.squareup.okhttp3:okhttp:5.3.2')

  final GradleProject gradleProject

  private final String agpVersion
  private final boolean disableReleaseTests

  UnitTestSingleVariantProject(String agpVersion, boolean disableReleaseTests) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.disableReleaseTests = disableReleaseTests
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { s ->
        s.sources = appSources()
        s.styles = AndroidStyleRes.DEFAULT
        s.colors = AndroidColorRes.DEFAULT
        s.manifest = AndroidManifest.app('my.android.app')
        s.withBuildScript { bs ->
          bs.plugins(androidApp(true))
          bs.android = defaultAndroidAppBlock(true)
          bs.additions = appFlavors()
          bs.androidComponents = androidComponentsForApp()
          bs.kotlin = Kotlin.DEFAULT
          bs.dependencies(
            appcompat('implementation'),
            JVM_LIB,
            junit('testImplementation'),
          )
        }
      }
      .withAndroidLibProject('lib') { lib ->
        lib.sources = libSources()
        lib.withBuildScript { bs ->
          bs.plugins(androidLib())
          bs.android = defaultAndroidLibBlock()
          bs.androidComponents = androidComponentsForLib()
          bs.kotlin = Kotlin.DEFAULT
          bs.dependencies(
            JVM_LIB,
            junit('testImplementation'),
          )
        }
      }
      .withSubproject('jvmlib') { jvmlib ->
        jvmlib.sources = jvmLibSources()
        jvmlib.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.dependencies(OKHTTP)
        }
      }
      .write()
  }

  private static String appFlavors() {
    '''\
    android {
      flavorDimensions += "element"
      productFlavors {
        create("fire") {
          dimension = "element"
        }
        create("water") {
          dimension = "element"
        }
      }
    }
    '''.stripIndent()
  }

  private AndroidComponents androidComponentsForApp() {
    if (disableReleaseTests) {
      AndroidComponents.of(
        '''\
      |  beforeVariants(selector().all()) {
      |    // Only enable tests for the debug build type
      |    hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE].enable = buildType == "debug"
      |  }'''.stripMargin('|')
      )
    } else {
      null
    }
  }

  private AndroidComponents androidComponentsForLib() {
    if (disableReleaseTests) {
      AndroidComponents.of(
        '''\
      |  beforeVariants(selector().all()) {
      |    // Only enable tests for the debug build type
      |    hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE].enable = buildType == "debug"
      |  }'''.stripMargin('|')
      )
    } else {
      null
    }
  }

  private static List<Source> appSources() {
    [
      Source.kotlin(
        '''\
        package com.example.android.app
                
        class App {
          fun magic() = 42
        }
        '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.android.app
        
        import okhttp3.OkHttpClient
        import org.junit.Test
        
        internal class LibTest {
          @Test fun test() {
            val client: OkHttpClient = TODO()
          }
        }
        '''.stripIndent()
      )
        .withSourceSet('test')
        .build(),
    ]
  }

  private static List<Source> libSources() {
    [
      Source.kotlin(
        '''\
        package com.example.android.lib
                
        class Lib {
          fun magic() = 42
        }
        '''.stripIndent()
      ).build(),
      Source.kotlin(
        '''\
        package com.example.android.lib
        
        import okhttp3.OkHttpClient
        import org.junit.Test
        
        internal class LibTest {
          @Test fun test() {
            val client: OkHttpClient = TODO()
          }
        }
        '''.stripIndent()
      )
        .withSourceSet('test')
        .build(),
    ]
  }

  private static List<Source> jvmLibSources() {
    [
      Source.kotlin(
        '''\
        package com.example.jvm.lib

        import okhttp3.OkHttpClient
        
        abstract class JvmLib {
          abstract fun client(): OkHttpClient
        }
        '''.stripIndent()
      ).build()
    ]
  }

  private static final Set<Advice> advice() {
    [
      Advice.ofRemove(projectCoordinates(JVM_LIB), JVM_LIB.configuration),
      Advice.ofAdd(moduleCoordinates(OKHTTP), 'testImplementation'),
      // This one is kind of annoying but expected. Since we're removing `:jvmlib`, which provides it, and okhttp has
      // a runtime capability, we're conservative and suggest keeping it on the classpath.
      Advice.ofAdd(moduleCoordinates(OKHTTP), 'runtimeOnly'),
    ]
  }

  private static final ProjectAdvice appAdvice() {
    projectAdviceForDependencies(':app', advice())
  }

  private static ProjectAdvice libAndroidAdvice() {
    projectAdviceForDependencies(':lib', advice())
  }

  private static ProjectAdvice libJvmAdvice() {
    projectAdviceForDependencies(':jvmlib', [] as Set<Advice>)
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    return [
      appAdvice(),
      libAndroidAdvice(),
      libJvmAdvice(),
    ]
  }
}
