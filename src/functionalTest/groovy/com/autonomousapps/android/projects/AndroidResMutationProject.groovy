// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.android.AndroidStringRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidResMutationProject extends AbstractAndroidProject {

  private static final RES_PATH = 'src/main/res/values/strings.xml'
  private static final LAYOUT_PATH = 'src/main/res/layout/activity_main.xml'

  final GradleProject gradleProject
  private final String agpVersion

  AndroidResMutationProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  void deleteRes() {
    getRes().delete()
    removeResReferenceFromSource()
  }

  void deleteLayout() {
    getLayout().delete()
  }

  private File getRes() {
    return gradleProject.projectDir(':res').resolve(RES_PATH).toFile()
  }

  private File getLayout() {
    return gradleProject.projectDir(':layouts').resolve(LAYOUT_PATH).toFile()
  }

  // We must do this to prevent compilation errors
  private void removeResReferenceFromSource() {
    def source = getSource()
    source.text = '''\
      package com.example
        
      import org.apache.commons.collections4.Bag
      
      class Library {
        private fun newBag(): Bag<String> = TODO()
      }'''.stripIndent()
  }

  private File getSource() {
    def subProjectDir = gradleProject.projectDir(':lib')
    def sourceInfo = sources.first()
    def sourceInfoRootPath = sourceInfo.relativeFilePath()
    return subProjectDir.resolve(sourceInfoRootPath).toFile()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.manifest = libraryManifest('com.example.lib')
        lib.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion)
          bs.android = defaultAndroidLibBlock(true, 'com.example.lib')
          bs.dependencies(
            implementation(':res'),
            commonsCollections('implementation'),
            kotlinStdLib('api'),
          )
        }
        lib.sources = sources
      }
      .withAndroidLibProject('res', 'com.example.lib.res') { res ->
        res.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib, Plugins.dependencyAnalysisNoVersion)
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib.res')
        }
        res.manifest = libraryManifest('com.example.lib.res')
        res.strings = AndroidStringRes.DEFAULT
      }
      .withAndroidLibProject('layouts', 'com.example.lib.layouts') { res ->
        res.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib, Plugins.dependencyAnalysisNoVersion)
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib.layouts')
          bs.dependencies(
            constraintLayout('implementation'),
          )
        }
        res.manifest = libraryManifest('com.example.lib.layouts')
        res.layouts = [androidLayout]
      }
      .write()
  }

  private sources = [
    Source.kotlin(
      '''\
      package com.example
        
      import android.content.Context
      import org.apache.commons.collections4.Bag
      import com.example.lib.res.R
      
      class Library {
        fun usesRes(context: Context) {
          context.getString(R.string.hello_world)
        }
        
        private fun newBag(): Bag<String> = TODO()
      }'''.stripIndent()
    ).build(),
  ]

  private AndroidLayout androidLayout = new AndroidLayout(
    'activity_main.xml',
    '''\
        <?xml version="1.0" encoding="utf-8"?>
        <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:app="http://schemas.android.com/apk/res-auto"
          xmlns:tools="http://schemas.android.com/tools"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          tools:context=".MainActivity"
          >
          <Button
            android:id="@+id/btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Hello!"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            />
        </androidx.constraintlayout.widget.ConstraintLayout>'''.stripIndent()
  )

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Advice removeRes = Advice.ofRemove(
    projectCoordinates(':res'),
    'implementation',
  )

  private final Advice removeConstraintLayout = Advice.ofRemove(
    moduleCoordinates('androidx.constraintlayout:constraintlayout:1.1.3'),
    'implementation',
  )

  final Set<ProjectAdvice> expectedOriginalBuildHealth = [
    emptyProjectAdviceFor(':layouts'),
    emptyProjectAdviceFor(':lib'),
    emptyProjectAdviceFor(':res'),
  ]

  final Set<ProjectAdvice> expectedDeletionBuildHealth = [
    emptyProjectAdviceFor(':layouts'),
    projectAdviceForDependencies(':lib', [removeRes] as Set<Advice>),
    emptyProjectAdviceFor(':res'),
  ]

  final Set<ProjectAdvice> expectedLayoutDeletionBuildHealth = [
    projectAdviceForDependencies(':layouts', [removeConstraintLayout] as Set<Advice>),
    emptyProjectAdviceFor(':lib'),
    emptyProjectAdviceFor(':res'),
  ]
}
