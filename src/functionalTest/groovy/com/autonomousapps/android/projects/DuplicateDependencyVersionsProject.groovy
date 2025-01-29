// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Dependency

import static com.autonomousapps.AdviceHelper.duplicateDependenciesReport
import static com.autonomousapps.AdviceHelper.resolvedDependenciesReport
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class DuplicateDependencyVersionsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DuplicateDependencyVersionsProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidAppPlugin
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            project('implementation', ':lib1'),
            new Dependency('api', 'junit:junit:4.12'),
          ]
        }
      }
      .withAndroidLibProject('lib1', 'com.example.lib1') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib1')
          bs.dependencies = [
            new Dependency('implementation', 'junit:junit:4.11'),
          ]
        }
      }
      .withAndroidLibProject('lib2', 'com.example.lib2') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib2')
          bs.dependencies = [
            new Dependency('api', 'junit:junit:4.13')
          ]
        }
      }
    // This "project" only exists for organizational reasons
      .withSubproject('group') {}
      .withSubproject('group:jvm-lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  Map<String, Set<String>> actualDuplicateDependencies() {
    return duplicateDependenciesReport(gradleProject)
  }

  List<String> actualResolvedDependenciesFor(String projectPath) {
    return resolvedDependenciesReport(gradleProject, projectPath)
  }

  String expectedOutput = '''\
    Your build uses 28 dependencies, representing 26 distinct 'libraries.' 1 libraries have multiple versions across the build. These are:
    * junit:junit:{4.11,4.12,4.13}'''.stripIndent()

  List<String> expectedResolvedDependenciesForApp = [
    'androidx.activity:activity:1.0.0',
    'androidx.annotation:annotation:1.1.0',
    'androidx.appcompat:appcompat-resources:1.1.0',
    'androidx.appcompat:appcompat:1.1.0',
    'androidx.arch.core:core-common:2.1.0',
    'androidx.arch.core:core-runtime:2.0.0',
    'androidx.collection:collection:1.1.0',
    'androidx.core:core:1.1.0',
    'androidx.cursoradapter:cursoradapter:1.0.0',
    'androidx.customview:customview:1.0.0',
    'androidx.drawerlayout:drawerlayout:1.0.0',
    'androidx.fragment:fragment:1.1.0',
    'androidx.interpolator:interpolator:1.0.0',
    'androidx.lifecycle:lifecycle-common:2.1.0',
    'androidx.lifecycle:lifecycle-livedata-core:2.0.0',
    'androidx.lifecycle:lifecycle-livedata:2.0.0',
    'androidx.lifecycle:lifecycle-runtime:2.1.0',
    'androidx.lifecycle:lifecycle-viewmodel:2.1.0',
    'androidx.loader:loader:1.0.0',
    'androidx.savedstate:savedstate:1.0.0',
    'androidx.vectordrawable:vectordrawable-animated:1.1.0',
    'androidx.vectordrawable:vectordrawable:1.1.0',
    'androidx.versionedparcelable:versionedparcelable:1.1.0',
    'androidx.viewpager:viewpager:1.0.0',
    'junit:junit:4.12',
    'org.hamcrest:hamcrest-core:1.3',
  ]

  List<String> expectedResolvedDependenciesForLib1 = [
    'junit:junit:4.11',
    'org.hamcrest:hamcrest-core:1.3',
  ]

  List<String> expectedResolvedDependenciesForLib2 = [
    'junit:junit:4.13',
    'org.hamcrest:hamcrest-core:1.3',
  ]

  List<String> expectedResolvedDependenciesForJvmLib = []
}
