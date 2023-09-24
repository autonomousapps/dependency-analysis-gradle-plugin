package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class AndroidProjectWithKmpDependencies extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final String additions

  AndroidProjectWithKmpDependencies(String agpVersion, String additions = '') {
    this.agpVersion = agpVersion
    this.additions = additions
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.additions = additions
        bs.buildscript = new BuildscriptBlock(
          Repository.DEFAULT,
          [androidPlugin(agpVersion)]
        )
      }
    }
    builder.withAndroidSubproject('app') { s ->
      s.manifest = AndroidManifest.app('com.example.MainApplication')
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin]
        bs.dependencies = [
          kotlinStdLib('implementation'),
          appcompat('implementation'),

          // Immutable collections JVM dep that should be corrected to the canonical target
          kotlinxImmutable('implementation', "-jvm"),

          // Coroutines Test JVM dep that should be
          // - Swapped with the core dep
          // - Core dep should use the canonical target
          kotlinxCoroutinesTest('implementation', "-jvm"),

          // A foundation compose dependency but we only use runtime APIs
          // This is an odd one because it will actually result in adding
          // the androidx compose dep. See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/919#issuecomment-1620643857
          composeMultiplatformFoundation('implementation'),
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'MainApplication', 'com/example',
      """\
        package com.example
        
        import android.app.Application
        import androidx.compose.runtime.Recomposer
        import kotlinx.coroutines.Dispatchers
        import kotlinx.collections.immutable.persistentListOf
      
        class MainApplication : Application() {
          override fun onCreate() {
            val list = persistentListOf(1)
            val recomposer = Recomposer(Dispatchers.IO)
          }
        }
      """
    )
  ]

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedAdvice() {
    return [
      addComposeRuntime(),
      removeComposeFoundation(),
      addKotlinxCoroutinesCore(),
      changeKotlinxCoroutinesTest(),
      // TODO need to make a new advice to replace KMP targets with canonical ones
      //  See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/919#issuecomment-1620684615
//      removeKotlinxImmutableJvm(),
//      addKotlinxImmutable(),
    ] as Set<Advice>
  }

  private static Advice addComposeRuntime() {
    return Advice.ofAdd(moduleCoordinates('androidx.compose.runtime:runtime', '1.1.0-beta04'), 'implementation')
  }

  private static Advice removeComposeFoundation() {
    return Advice.ofRemove(moduleCoordinates('org.jetbrains.compose.foundation:foundation', '1.0.1'), 'implementation')
  }

  private static Advice addKotlinxCoroutinesCore() {
    return Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-core', '1.6.0'), 'implementation')
  }

  // In practice we don't actually want to keep this, but because it has a serviceloader DAGP will
  // conservatively keep it as runtimeOnly instead of removing
  private static Advice changeKotlinxCoroutinesTest() {
    return Advice.ofChange(
      moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm', '1.6.0'),
      'implementation',
      'runtimeOnly'
    )
  }

  private static Advice removeKotlinxImmutableJvm() {
    return Advice.ofRemove(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm', '0.3.5'), 'implementation')
  }

  private static Advice addKotlinxImmutable() {
    return Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-collections-immutable', '0.3.5'), 'implementation')
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', expectedAdvice()),
  ]
}
