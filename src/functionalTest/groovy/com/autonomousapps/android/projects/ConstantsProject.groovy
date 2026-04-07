// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class ConstantsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ConstantsProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidApp()
          bs.android = defaultAndroidAppBlock(true, 'com.example.app')
          bs.dependencies(
            implementation(':lib'),
            implementation(':lib2'),
            implementation(':libstar'),
            kotlinStdLib('implementation'),
            appcompat('implementation'),
          )
        }
        app.sources = appSource
        app.manifest = AndroidManifest.app()
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
      }
      .withAndroidLibProject('lib', 'mutual.aid.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins(androidLib())
          bs.android = defaultAndroidLibBlock(true, 'mutual.aid.lib')
          bs.dependencies(kotlinStdLib('implementation'))
        }
        lib.sources = libSource
      }
      .withAndroidLibProject('lib2', 'mutual.aid.lib2') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins(androidLib())
          bs.android = defaultAndroidLibBlock(true, 'mutual.aid.lib2')
          bs.dependencies(kotlinStdLib('implementation'))
        }
        lib.sources = lib2Source
      }
      .withAndroidLibProject('libstar', 'mutual.aid.libstar') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins(androidLib())
          bs.android = defaultAndroidLibBlock(true, 'mutual.aid.libstar')
          bs.dependencies(kotlinStdLib('implementation'))
        }
        lib.sources = libstarSource
      }
      .write()
  }

  private List<Source> appSource = [
    Source.kotlin(
      '''\
        package mutual.aid
        
        import androidx.appcompat.app.AppCompatActivity
        import mutual.aid.lib.Producer
        import mutual.aid.lib2.BuildConfig.DEBUG
        import mutual.aid.libstar.*
        
        class MainActivity : AppCompatActivity() {
          fun magic() {
            if (DEBUG) {
              println("Magic = " + Producer.MAGIC)
              println(ONE)
              println(TWO)
              println(THREE)
            }
          }
        }'''.stripIndent()
    ).build()
  ]

  private List<Source> libSource = [
    Source.kotlin(
      '''\
        package mutual.aid.lib
        
        object Producer {
          const val MAGIC = 42
        }'''.stripIndent()
    ).build()
  ]

  private List<Source> lib2Source = [
    Source.kotlin(
      '''\
        package mutual.aid.lib2
        
        object BuildConfig {
          const val DEBUG = true
        }'''.stripIndent()
    ).build()
  ]

  private List<Source> libstarSource = [
    Source.kotlin(
      '''\
        package mutual.aid.libstar
        
        const val ONE = 1
        const val TWO = 2
        const val THREE = 3'''.stripIndent()
    )
      .withPath('mutual.aid.lib2', 'libstar')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(':app', ':lib', ':lib2', ':libstar')
}
