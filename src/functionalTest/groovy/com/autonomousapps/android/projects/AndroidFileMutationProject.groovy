// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidFileMutationProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  AndroidFileMutationProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  void deleteSourceFile() {
    def subProjectDir = gradleProject.projectDir(gradleProject.subprojects.first())
    def sourceInfo = sources.first()
    def sourceInfoRootPath = "src/main/kotlin/" + sourceInfo.path + "/" + sourceInfo.name + ".kt"
    def sourceFile = subProjectDir.resolve(sourceInfoRootPath).toFile()
    sourceFile.delete()
  }

  void renameAndRewriteSourceFile() {
    def subProjectDir = gradleProject.projectDir(gradleProject.subprojects.first())
    def sourceInfo = sources.first()
    def sourceInfoRootPath = "src/main/kotlin/" + sourceInfo.path + "/" + sourceInfo.name + ".kt"
    def sourceFile = subProjectDir.resolve(sourceInfoRootPath).toFile()
    def newFile = new File(sourceFile.parentFile, "NewLibrary.kt")
    sourceFile.write(
      """\
        package com.example
        
        import android.content.Context
      
        class NewLibrary {
          fun provideContext(context: Context): Context {
            return context
          }
        }
      """
    )
    sourceFile.renameTo(newFile)
    println("${sourceFile.absolutePath} --> ${newFile.absolutePath}")
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { l ->
        l.manifest = AndroidManifest.defaultLib('com.example.lib')
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock()
          bs.dependencies = [
            constraintLayout('api'),
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
        import androidx.constraintlayout.widget.Group
      
        class Library {
          fun provideGroup(context: Context): Group {
            return Group(context)
          }
        }
      """
    ),
    new Source(
      SourceType.KOTLIN, 'Library2', 'com/example',
      """\
        package com.example
        
        import android.content.Context
        import android.telephony.TelephonyManager
        import androidx.core.content.getSystemService
      
        class Library2 {
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

  private final Set<Advice> libAdvice = [
    Advice.ofRemove(moduleCoordinates("androidx.constraintlayout:constraintlayout:1.1.3"), "api"),
  ] as Set<Advice>

  final Set<ProjectAdvice> expectedDeletionBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]

  final Set<ProjectAdvice> expectedRenameBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]

  final Set<ProjectAdvice> expectedOriginalBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
