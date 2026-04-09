// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class DominanceTreeProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DominanceTreeProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidApp(false)
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            project('implementation', ':lib'),
          ]
        }
      }
      .withAndroidLibProject('lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLib(false)
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
        }
      }.write()
  }

  List<String> actualTree() {
    return gradleProject.rootDir.toPath()
      .resolve('app/build/reports/dependency-analysis/debugMain/graph/graph-dominator.txt')
      .readLines()
  }

  final expectedTree = """\
      10.01 MiB :app
      +--- 8.32 MiB (1.69 MiB) androidx.appcompat:appcompat:1.7.1
      |    +--- 2.70 MiB (2.63 MiB) androidx.core:core:1.13.0
      |    |    \\--- 0.07 MiB androidx.versionedparcelable:versionedparcelable:1.1.1
      |    +--- 1.43 MiB (0.02 MiB) org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4
      |    |    +--- 1.41 MiB org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
      |    |    |    \\--- 1.41 MiB (1.41 MiB) org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4
      |    |    |         \\--- org.jetbrains.kotlin:kotlin-stdlib-common:2.2.21
      |    |    \\--- 0.00 MiB (0.00 MiB) org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0
      |    |         \\--- 0.00 MiB org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0
      |    +--- 0.86 MiB (0.62 MiB) androidx.fragment:fragment:1.5.4
      |    |    +--- 0.13 MiB (0.08 MiB) androidx.loader:loader:1.0.0
      |    |    |    \\--- 0.05 MiB (0.04 MiB) androidx.lifecycle:lifecycle-livedata:2.6.2
      |    |    |         \\--- 0.01 MiB androidx.arch.core:core-runtime:2.1.0
      |    |    \\--- 0.11 MiB androidx.viewpager:viewpager:1.0.0
      |    +--- 0.37 MiB androidx.core:core-ktx:1.2.0
      |    +--- 0.36 MiB androidx.activity:activity:1.8.0
      |    +--- 0.32 MiB (0.15 MiB) androidx.appcompat:appcompat-resources:1.7.1
      |    |    +--- 0.09 MiB (0.08 MiB) androidx.vectordrawable:vectordrawable-animated:1.1.0
      |    |    |    \\--- 0.01 MiB androidx.interpolator:interpolator:1.0.0
      |    |    \\--- 0.08 MiB androidx.vectordrawable:vectordrawable:1.1.0
      |    +--- 0.08 MiB androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2
      |    +--- 0.08 MiB androidx.lifecycle:lifecycle-viewmodel:2.6.2
      |    +--- 0.07 MiB androidx.customview:customview:1.0.0
      |    +--- 0.07 MiB androidx.drawerlayout:drawerlayout:1.0.0
      |    +--- 0.05 MiB androidx.annotation:annotation:1.6.0
      |    |    \\--- 0.05 MiB androidx.annotation:annotation-jvm:1.6.0
      |    +--- 0.05 MiB androidx.lifecycle:lifecycle-common:2.6.2
      |    +--- 0.04 MiB androidx.collection:collection:1.1.0
      |    +--- 0.04 MiB androidx.lifecycle:lifecycle-runtime:2.6.2
      |    +--- 0.04 MiB androidx.savedstate:savedstate:1.2.1
      |    +--- 0.02 MiB androidx.cursoradapter:cursoradapter:1.0.0
      |    +--- 0.02 MiB androidx.lifecycle:lifecycle-livedata-core:2.6.2
      |    +--- 0.01 MiB androidx.arch.core:core-common:2.2.0
      |    \\--- 0.01 MiB androidx.annotation:annotation-experimental:1.4.0
      +--- 1.70 MiB (1.68 MiB) org.jetbrains.kotlin:kotlin-stdlib:2.2.21
      |    \\--- 0.02 MiB org.jetbrains:annotations:13.0
      \\--- 0.00 MiB :lib""".stripIndent().readLines()
}
