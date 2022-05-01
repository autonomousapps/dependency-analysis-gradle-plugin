package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class AndroidKotlinInlineProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidKotlinInlineProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('lib') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = [
          Dependency.coreKtx('implementation'),
          Dependency.core('implementation'),
          Dependency.kotlinStdLib('api')
        ]
      }
      l.sources = sources
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example',
      """\
        package com.example
        
        import android.content.Context
        import android.telephony.TelephonyManager
        import androidx.core.content.getSystemService
      
        class Library {
          fun provideTelephonyManager(context: Context): TelephonyManager {
            return context.getSystemService()!!
          }
        }
      """
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
