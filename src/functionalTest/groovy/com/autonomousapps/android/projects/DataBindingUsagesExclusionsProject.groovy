// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class DataBindingUsagesExclusionsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final boolean excludeDataBinderMapper

  DataBindingUsagesExclusionsProject(String agpVersion, boolean excludeDataBinderMapper) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.excludeDataBinderMapper = excludeDataBinderMapper
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { root ->
        root.withBuildScript { bs ->
          if (excludeDataBinderMapper) {
            bs.withGroovy("""\
            dependencyAnalysis {
              usages {
                exclusions {
                  excludeClasses(".*\\\\.DataBinderMapperImpl\\\$")
                }
              }
            }""")
          }
        }
      }
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = [
            Plugins.androidApp,
            Plugins.kotlinAndroidNoVersion,
            Plugins.dependencyAnalysisNoVersion,
          ]
          bs.android = defaultAndroidAppBlock(true, 'com.example.app')
          bs.dependencies = appDependencies
          bs.withGroovy('android.buildFeatures.dataBinding true')
        }
        app.manifest = AndroidManifest.defaultLib('com.example.app')
        app.sources = appSources
      }
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [
            Plugins.androidLib,
            Plugins.kotlinAndroidNoVersion,
            Plugins.kotlinKaptNoVersion,
            Plugins.dependencyAnalysisNoVersion,
          ]
          bs.android = defaultAndroidLibBlock(true, 'com.example.lib')
          bs.withGroovy('android.buildFeatures.dataBinding true')
        }
        lib.sources = libSources
        lib.withFile('src/main/res/layout/hello.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:binding="http://schemas.android.com/apk/res-auto">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              binding:text="@{`Hello`}" />

          </layout>""".stripIndent()
        )
      }
      .write()
  }

  private appSources = [
    new Source(
      SourceType.KOTLIN, 'MainActivity', 'com/example/app',
      """
        package com.example.app
        
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
        }""".stripIndent()
    )
  ]

  private List<Dependency> appDependencies = [
    appcompat('implementation'),
    project('implementation', ':lib'),
  ]

  private libSources = [
    new Source(
      SourceType.KOTLIN, 'Bindings', 'com/example/lib',
      """
        package com.example.lib
        
        import android.widget.TextView
        import androidx.databinding.BindingAdapter
        
        @BindingAdapter("text")
        fun setText(view: TextView, text: String) {
          view.text = text
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<ProjectAdvice> expectedBuildHealthWithExclusions = [
    projectAdviceForDependencies(
      ':app',
      [Advice.ofRemove(projectCoordinates(':lib'), 'implementation')] as Set<Advice>,
    ),
    emptyProjectAdviceFor(':lib'),
  ]

  private final Set<ProjectAdvice> expectedBuildHealthWithoutExclusions = [
    emptyProjectAdviceFor(':app'),
    emptyProjectAdviceFor(':lib'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = excludeDataBinderMapper
    ? expectedBuildHealthWithExclusions
    : expectedBuildHealthWithoutExclusions
}
