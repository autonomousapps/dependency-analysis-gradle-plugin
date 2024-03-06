// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidKotlinInlineProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidKotlinInlineProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { l ->
        l.manifest = AndroidManifest.defaultLib('com.example.lib')
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroid, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock()
          bs.dependencies = [
            coreKtx('implementation'),
            core('implementation'),
            kotlinStdLib('api')
          ]
        }
        l.sources = sources
      }.write()
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
