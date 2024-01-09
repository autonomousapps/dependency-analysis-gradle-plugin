// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.constraintLayout
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.recyclerView

final class ResDuplicateAttrProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ResDuplicateAttrProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('lib-a', 'com.example.lib_a') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib]
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib_a')
          bs.dependencies(
            project('implementation', ':lib-b'),
            constraintLayout('implementation'),
          )
        }
        lib.manifest = libraryManifest('com.example.lib_a')
        lib.sources = libASources
        lib.layouts = [
          new AndroidLayout(
            'sample_layout.xml',
            '''\
            <?xml version="1.0" encoding="utf-8"?>
            <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
              xmlns:app="http://schemas.android.com/apk/res-auto"
              android:orientation="vertical"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
          
              <ImageView
                android:id="@+id/icon"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
          
              <TextView
                android:id="@+id/textView"
                app:layout_constraintTop_toTopOf="@id/icon"
                app:layout_constraintStart_toEndOf="@id/icon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
          
            </androidx.constraintlayout.widget.ConstraintLayout>'''.stripIndent()
          )
        ]
      }
      .withAndroidLibProject('lib-b', 'com.example.lib_b') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib]
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib_b')
          bs.dependencies(recyclerView('api'))
        }
        lib.manifest = AndroidManifest.defaultLib('com.example.lib_b')
        lib.sources = libBSources
      }
      .write()
  }

  private List<Source> libASources = [
    Source.java(
      '''\
        package com.example.lib_a;

        import com.example.lib_b.LibB;

        class LibA {
          private LibB libb;
        }
      '''
    )
      .withPath('com.example.lib_a', 'LibA')
      .build()
  ]

  private List<Source> libBSources = [
    Source.java(
      '''\
        package com.example.lib_b;

        import androidx.recyclerview.widget.RecyclerView.ViewHolder;

        public class LibB {
          public ViewHolder holder;
        }
      '''
    )
      .withPath('com.example.lib_b', 'LibB')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(':lib-a', ':lib-b')
}
