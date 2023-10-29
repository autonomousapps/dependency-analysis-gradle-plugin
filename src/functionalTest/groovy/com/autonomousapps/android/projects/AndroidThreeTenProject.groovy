package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.androidPlugin
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidThreeTenProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final String additions

  AndroidThreeTenProject(String agpVersion, String additions = '') {
    super(agpVersion)
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
        bs.plugins = [Plugins.androidApp, Plugins.kotlinAndroid]
        bs.android = androidAppBlock()
        bs.dependencies = [
          kotlinStdLib('implementation'),
          appcompat('implementation'),
          jwThreeTenAbp('implementation')
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
        import com.jakewharton.threetenabp.AndroidThreeTen
        import org.threeten.bp.Clock
      
        class MainApplication : Application() {
          override fun onCreate() {
            AndroidThreeTen.init(this)
            
            var clock: Clock? = null
          }
        }
      """
    )
  ]

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedAdvice() {
    return [addThreeTenBp()] as Set<Advice>
  }

  private static Advice addThreeTenBp() {
    return Advice.ofAdd(moduleCoordinates('org.threeten:threetenbp', '1.4.4'), 'implementation')
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> appAdvice = [addThreeTenBp()]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice),
  ]

  final Set<ProjectAdvice> expectedBundleBuildHealth = [
    emptyProjectAdviceFor(':app'),
  ]
}
