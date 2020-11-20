package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.AndroidBlock
import com.autonomousapps.kit.BuildscriptBlock
import com.autonomousapps.kit.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.GradleProperties
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

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

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
  }

  final List<Advice> expectedAdvice = []
}
