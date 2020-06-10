package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.kit.*

import static com.autonomousapps.kit.Dependency.*

final class AndroidThreeTenProject extends AbstractProject {

  private static final ANDROID_THREE_TEN_BP = new Dependency(
    'com.jakewharton.threetenabp:threetenabp',
    '1.2.4',
    'implementation'
  )
  private static final THREE_TEN_BP = new Dependency(
    'org.threeten:threetenbp',
    '1.4.4',
    null
  )

  final GradleProject gradleProject
  private final String agpVersion
  private final String additions

  AndroidThreeTenProject(String agpVersion, String additions = '') {
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
    return Advice.ofAdd(
      new TransitiveDependency(THREE_TEN_BP, [ANDROID_THREE_TEN_BP] as Set<Dependency>, [] as Set<String>),
      'implementation'
    )
  }
}
