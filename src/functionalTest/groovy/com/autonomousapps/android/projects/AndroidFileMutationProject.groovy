// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
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
    getSourceFile().delete()
  }

  void renameAndRewriteSourceFile() {
    def sourceFile = getSourceFile()
    def newFile = new File(sourceFile.parentFile, "NewLibrary.kt")
    sourceFile.write(
      """\
        package com.example
        
        import android.content.Context
        // This import is unused, but we're using it to validate _source_ parsing via `CodeSourceExploderTask`. The
        // advice should _not_ recommend removing the commons-collections dep, given this import statement.
        import org.apache.commons.collections4.Bag

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

  void mutateSourceFile() {
    def sourceFile = getSourceFile()
    sourceFile.text = "private fun foo(): Nothing = TODO()"
  }

  private File getSourceFile() {
    def subProjectDir = gradleProject.projectDir(gradleProject.subprojects.first())
    def sourceInfo = sources.first()
    def sourceInfoRootPath = sourceInfo.relativeFilePath()
    return subProjectDir.resolve(sourceInfoRootPath).toFile()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('lib') { l ->
        l.manifest = AndroidManifest.defaultLib('com.example.lib')
        l.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock()
          bs.dependencies = [
            commonsCollections('implementation'),
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
    Source.kotlin(
      '''\
      package com.example
        
      import android.content.Context
      import androidx.constraintlayout.widget.Group
      
      class Library {
        fun provideGroup(context: Context): Group {
          return Group(context)
        }
      }'''.stripIndent()
    ).build(),
    Source.kotlin(
      '''\
      package com.example
      
      import android.content.Context
      import android.telephony.TelephonyManager
      import androidx.core.content.getSystemService
    
      class Library2 {
        fun provideTelephonyManager(context: Context): TelephonyManager {
          return context.getSystemService()!!
        }
      }'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Advice removeConstraintLayout = Advice.ofRemove(
    moduleCoordinates("androidx.constraintlayout:constraintlayout:1.1.3"),
    "api"
  )

  private final Advice removeCommonsCollections = Advice.ofRemove(
    moduleCoordinates("org.apache.commons:commons-collections4:4.4"),
    "implementation"
  )

  final Set<ProjectAdvice> expectedOriginalBuildHealth = [
    projectAdviceForDependencies(':lib', [removeCommonsCollections] as Set<Advice>),
  ]

  final Set<ProjectAdvice> expectedDeletionBuildHealth = [
    projectAdviceForDependencies(':lib', [removeConstraintLayout, removeCommonsCollections] as Set<Advice>),
  ]

  final Set<ProjectAdvice> expectedRenameBuildHealth = [
    projectAdviceForDependencies(':lib', [removeConstraintLayout] as Set<Advice>),
  ]

  final Set<ProjectAdvice> expectedMutationBuildHealth = [
    projectAdviceForDependencies(':lib', [removeConstraintLayout, removeCommonsCollections] as Set<Advice>),
  ]
}
