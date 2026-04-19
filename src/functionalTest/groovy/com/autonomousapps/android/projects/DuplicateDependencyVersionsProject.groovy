// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.internal.OutputPathsKt
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
          bs.plugins = androidApp(false)
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            project('implementation', ':lib1'),
            new Dependency('api', 'junit:junit:4.12'),
          ]
        }
      }
      .withAndroidLibProject('lib1') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLib(false)
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib1')
          bs.dependencies = [
            new Dependency('implementation', 'junit:junit:4.11'),
          ]
        }
      }
      .withAndroidLibProject('lib2') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLib(false)
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

  String actualResolvedAllLibsFor(String projectPath) {
    return gradleProject.singleArtifact(projectPath, OutputPathsKt.getResolvedVersionsTomlPath()).asPath.text
  }

  String actualAllDependencies() {
    return gradleProject.singleArtifact(':', OutputPathsKt.getAllLibsVersionsTomlPath()).asPath.text
  }

  String expectedAllDependencies = '''\
    [libraries]
    androidx-activity-activity-1-8-0 = { module = "androidx.activity:activity", version = "1.8.0" }
    androidx-annotation-annotation-experimental-1-4-0 = { module = "androidx.annotation:annotation-experimental", version = "1.4.0" }
    androidx-annotation-annotation-jvm-1-6-0 = { module = "androidx.annotation:annotation-jvm", version = "1.6.0" }
    androidx-annotation-annotation-1-6-0 = { module = "androidx.annotation:annotation", version = "1.6.0" }
    androidx-appcompat-appcompat-resources-1-7-1 = { module = "androidx.appcompat:appcompat-resources", version = "1.7.1" }
    androidx-appcompat-appcompat-1-7-1 = { module = "androidx.appcompat:appcompat", version = "1.7.1" }
    androidx-arch-core-core-common-2-2-0 = { module = "androidx.arch.core:core-common", version = "2.2.0" }
    androidx-arch-core-core-runtime-2-1-0 = { module = "androidx.arch.core:core-runtime", version = "2.1.0" }
    androidx-arch-core-core-runtime-2-2-0 = { module = "androidx.arch.core:core-runtime", version = "2.2.0" }
    androidx-collection-collection-1-1-0 = { module = "androidx.collection:collection", version = "1.1.0" }
    androidx-concurrent-concurrent-futures-1-1-0 = { module = "androidx.concurrent:concurrent-futures", version = "1.1.0" }
    androidx-core-core-ktx-1-13-0 = { module = "androidx.core:core-ktx", version = "1.13.0" }
    androidx-core-core-ktx-1-2-0 = { module = "androidx.core:core-ktx", version = "1.2.0" }
    androidx-core-core-1-13-0 = { module = "androidx.core:core", version = "1.13.0" }
    androidx-cursoradapter-cursoradapter-1-0-0 = { module = "androidx.cursoradapter:cursoradapter", version = "1.0.0" }
    androidx-customview-customview-1-0-0 = { module = "androidx.customview:customview", version = "1.0.0" }
    androidx-drawerlayout-drawerlayout-1-0-0 = { module = "androidx.drawerlayout:drawerlayout", version = "1.0.0" }
    androidx-emoji2-emoji2-views-helper-1-3-0 = { module = "androidx.emoji2:emoji2-views-helper", version = "1.3.0" }
    androidx-emoji2-emoji2-1-3-0 = { module = "androidx.emoji2:emoji2", version = "1.3.0" }
    androidx-fragment-fragment-1-5-4 = { module = "androidx.fragment:fragment", version = "1.5.4" }
    androidx-interpolator-interpolator-1-0-0 = { module = "androidx.interpolator:interpolator", version = "1.0.0" }
    androidx-lifecycle-lifecycle-common-2-6-2 = { module = "androidx.lifecycle:lifecycle-common", version = "2.6.2" }
    androidx-lifecycle-lifecycle-livedata-core-2-6-2 = { module = "androidx.lifecycle:lifecycle-livedata-core", version = "2.6.2" }
    androidx-lifecycle-lifecycle-livedata-2-6-2 = { module = "androidx.lifecycle:lifecycle-livedata", version = "2.6.2" }
    androidx-lifecycle-lifecycle-process-2-6-2 = { module = "androidx.lifecycle:lifecycle-process", version = "2.6.2" }
    androidx-lifecycle-lifecycle-runtime-2-6-2 = { module = "androidx.lifecycle:lifecycle-runtime", version = "2.6.2" }
    androidx-lifecycle-lifecycle-viewmodel-savedstate-2-6-2 = { module = "androidx.lifecycle:lifecycle-viewmodel-savedstate", version = "2.6.2" }
    androidx-lifecycle-lifecycle-viewmodel-2-6-2 = { module = "androidx.lifecycle:lifecycle-viewmodel", version = "2.6.2" }
    androidx-loader-loader-1-0-0 = { module = "androidx.loader:loader", version = "1.0.0" }
    androidx-profileinstaller-profileinstaller-1-3-1 = { module = "androidx.profileinstaller:profileinstaller", version = "1.3.1" }
    androidx-resourceinspection-resourceinspection-annotation-1-0-1 = { module = "androidx.resourceinspection:resourceinspection-annotation", version = "1.0.1" }
    androidx-savedstate-savedstate-1-2-1 = { module = "androidx.savedstate:savedstate", version = "1.2.1" }
    androidx-startup-startup-runtime-1-1-1 = { module = "androidx.startup:startup-runtime", version = "1.1.1" }
    androidx-tracing-tracing-1-0-0 = { module = "androidx.tracing:tracing", version = "1.0.0" }
    androidx-vectordrawable-vectordrawable-animated-1-1-0 = { module = "androidx.vectordrawable:vectordrawable-animated", version = "1.1.0" }
    androidx-vectordrawable-vectordrawable-1-1-0 = { module = "androidx.vectordrawable:vectordrawable", version = "1.1.0" }
    androidx-versionedparcelable-versionedparcelable-1-1-1 = { module = "androidx.versionedparcelable:versionedparcelable", version = "1.1.1" }
    androidx-viewpager-viewpager-1-0-0 = { module = "androidx.viewpager:viewpager", version = "1.0.0" }
    com-google-guava-listenablefuture-1-0 = { module = "com.google.guava:listenablefuture", version = "1.0" }
    junit-junit-4-11 = { module = "junit:junit", version = "4.11" }
    junit-junit-4-12 = { module = "junit:junit", version = "4.12" }
    junit-junit-4-13 = { module = "junit:junit", version = "4.13" }
    org-hamcrest-hamcrest-core-1-3 = { module = "org.hamcrest:hamcrest-core", version = "1.3" }
    org-jetbrains-kotlin-kotlin-stdlib-common-2-3-20 = { module = "org.jetbrains.kotlin:kotlin-stdlib-common", version = "2.3.20" }
    org-jetbrains-kotlin-kotlin-stdlib-jdk7-1-8-0 = { module = "org.jetbrains.kotlin:kotlin-stdlib-jdk7", version = "1.8.0" }
    org-jetbrains-kotlin-kotlin-stdlib-jdk8-1-8-0 = { module = "org.jetbrains.kotlin:kotlin-stdlib-jdk8", version = "1.8.0" }
    org-jetbrains-kotlin-kotlin-stdlib-2-3-20 = { module = "org.jetbrains.kotlin:kotlin-stdlib", version = "2.3.20" }
    org-jetbrains-kotlinx-kotlinx-coroutines-android-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version = "1.6.4" }
    org-jetbrains-kotlinx-kotlinx-coroutines-core-jvm-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm", version = "1.6.4" }
    org-jetbrains-kotlinx-kotlinx-coroutines-core-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.6.4" }
    org-jetbrains-annotations-13-0 = { module = "org.jetbrains:annotations", version = "13.0" }
    '''.stripIndent()

  String expectedOutput = '''\
    Your build uses 51 dependencies, representing 47 distinct 'libraries.' 3 libraries have multiple versions across the build. These are:
    * androidx.arch.core:core-runtime:{2.1.0,2.2.0}
    * androidx.core:core-ktx:{1.2.0,1.13.0}
    * junit:junit:{4.11,4.12,4.13}'''.stripIndent()

  List<String> expectedResolvedDependenciesForApp = [
    'androidx.activity:activity:1.8.0',
    'androidx.annotation:annotation-experimental:1.4.0',
    'androidx.annotation:annotation-jvm:1.6.0',
    'androidx.annotation:annotation:1.6.0',
    'androidx.appcompat:appcompat-resources:1.7.1',
    'androidx.appcompat:appcompat:1.7.1',
    'androidx.arch.core:core-common:2.2.0',
    'androidx.arch.core:core-runtime:2.1.0',
    'androidx.arch.core:core-runtime:2.2.0',
    'androidx.collection:collection:1.1.0',
    'androidx.concurrent:concurrent-futures:1.1.0',
    'androidx.core:core-ktx:1.13.0',
    'androidx.core:core-ktx:1.2.0',
    'androidx.core:core:1.13.0',
    'androidx.cursoradapter:cursoradapter:1.0.0',
    'androidx.customview:customview:1.0.0',
    'androidx.drawerlayout:drawerlayout:1.0.0',
    'androidx.emoji2:emoji2-views-helper:1.3.0',
    'androidx.emoji2:emoji2:1.3.0',
    'androidx.fragment:fragment:1.5.4',
    'androidx.interpolator:interpolator:1.0.0',
    'androidx.lifecycle:lifecycle-common:2.6.2',
    'androidx.lifecycle:lifecycle-livedata-core:2.6.2',
    'androidx.lifecycle:lifecycle-livedata:2.6.2',
    'androidx.lifecycle:lifecycle-process:2.6.2',
    'androidx.lifecycle:lifecycle-runtime:2.6.2',
    'androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2',
    'androidx.lifecycle:lifecycle-viewmodel:2.6.2',
    'androidx.loader:loader:1.0.0',
    'androidx.profileinstaller:profileinstaller:1.3.1',
    'androidx.resourceinspection:resourceinspection-annotation:1.0.1',
    'androidx.savedstate:savedstate:1.2.1',
    'androidx.startup:startup-runtime:1.1.1',
    'androidx.tracing:tracing:1.0.0',
    'androidx.vectordrawable:vectordrawable-animated:1.1.0',
    'androidx.vectordrawable:vectordrawable:1.1.0',
    'androidx.versionedparcelable:versionedparcelable:1.1.1',
    'androidx.viewpager:viewpager:1.0.0',
    'com.google.guava:listenablefuture:1.0',
    'junit:junit:4.12',
    'org.hamcrest:hamcrest-core:1.3',
    'org.jetbrains.kotlin:kotlin-stdlib-common:1.8.22',
    'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.21',
    'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21',
    'org.jetbrains.kotlin:kotlin-stdlib:1.8.22',
    'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4',
    'org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4',
    'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4',
    'org.jetbrains:annotations:13.0',
  ]

  String expectedResolvedAllLibsForApp = '''\
    [libraries]
    androidx-activity-activity-1-8-0 = { module = "androidx.activity:activity", version = "1.8.0" }
    androidx-annotation-annotation-experimental-1-4-0 = { module = "androidx.annotation:annotation-experimental", version = "1.4.0" }
    androidx-annotation-annotation-jvm-1-6-0 = { module = "androidx.annotation:annotation-jvm", version = "1.6.0" }
    androidx-annotation-annotation-1-6-0 = { module = "androidx.annotation:annotation", version = "1.6.0" }
    androidx-appcompat-appcompat-resources-1-7-1 = { module = "androidx.appcompat:appcompat-resources", version = "1.7.1" }
    androidx-appcompat-appcompat-1-7-1 = { module = "androidx.appcompat:appcompat", version = "1.7.1" }
    androidx-arch-core-core-common-2-2-0 = { module = "androidx.arch.core:core-common", version = "2.2.0" }
    androidx-arch-core-core-runtime-2-1-0 = { module = "androidx.arch.core:core-runtime", version = "2.1.0" }
    androidx-arch-core-core-runtime-2-2-0 = { module = "androidx.arch.core:core-runtime", version = "2.2.0" }
    androidx-collection-collection-1-1-0 = { module = "androidx.collection:collection", version = "1.1.0" }
    androidx-concurrent-concurrent-futures-1-1-0 = { module = "androidx.concurrent:concurrent-futures", version = "1.1.0" }
    androidx-core-core-ktx-1-13-0 = { module = "androidx.core:core-ktx", version = "1.13.0" }
    androidx-core-core-ktx-1-2-0 = { module = "androidx.core:core-ktx", version = "1.2.0" }
    androidx-core-core-1-13-0 = { module = "androidx.core:core", version = "1.13.0" }
    androidx-cursoradapter-cursoradapter-1-0-0 = { module = "androidx.cursoradapter:cursoradapter", version = "1.0.0" }
    androidx-customview-customview-1-0-0 = { module = "androidx.customview:customview", version = "1.0.0" }
    androidx-drawerlayout-drawerlayout-1-0-0 = { module = "androidx.drawerlayout:drawerlayout", version = "1.0.0" }
    androidx-emoji2-emoji2-views-helper-1-3-0 = { module = "androidx.emoji2:emoji2-views-helper", version = "1.3.0" }
    androidx-emoji2-emoji2-1-3-0 = { module = "androidx.emoji2:emoji2", version = "1.3.0" }
    androidx-fragment-fragment-1-5-4 = { module = "androidx.fragment:fragment", version = "1.5.4" }
    androidx-interpolator-interpolator-1-0-0 = { module = "androidx.interpolator:interpolator", version = "1.0.0" }
    androidx-lifecycle-lifecycle-common-2-6-2 = { module = "androidx.lifecycle:lifecycle-common", version = "2.6.2" }
    androidx-lifecycle-lifecycle-livedata-core-2-6-2 = { module = "androidx.lifecycle:lifecycle-livedata-core", version = "2.6.2" }
    androidx-lifecycle-lifecycle-livedata-2-6-2 = { module = "androidx.lifecycle:lifecycle-livedata", version = "2.6.2" }
    androidx-lifecycle-lifecycle-process-2-6-2 = { module = "androidx.lifecycle:lifecycle-process", version = "2.6.2" }
    androidx-lifecycle-lifecycle-runtime-2-6-2 = { module = "androidx.lifecycle:lifecycle-runtime", version = "2.6.2" }
    androidx-lifecycle-lifecycle-viewmodel-savedstate-2-6-2 = { module = "androidx.lifecycle:lifecycle-viewmodel-savedstate", version = "2.6.2" }
    androidx-lifecycle-lifecycle-viewmodel-2-6-2 = { module = "androidx.lifecycle:lifecycle-viewmodel", version = "2.6.2" }
    androidx-loader-loader-1-0-0 = { module = "androidx.loader:loader", version = "1.0.0" }
    androidx-profileinstaller-profileinstaller-1-3-1 = { module = "androidx.profileinstaller:profileinstaller", version = "1.3.1" }
    androidx-resourceinspection-resourceinspection-annotation-1-0-1 = { module = "androidx.resourceinspection:resourceinspection-annotation", version = "1.0.1" }
    androidx-savedstate-savedstate-1-2-1 = { module = "androidx.savedstate:savedstate", version = "1.2.1" }
    androidx-startup-startup-runtime-1-1-1 = { module = "androidx.startup:startup-runtime", version = "1.1.1" }
    androidx-tracing-tracing-1-0-0 = { module = "androidx.tracing:tracing", version = "1.0.0" }
    androidx-vectordrawable-vectordrawable-animated-1-1-0 = { module = "androidx.vectordrawable:vectordrawable-animated", version = "1.1.0" }
    androidx-vectordrawable-vectordrawable-1-1-0 = { module = "androidx.vectordrawable:vectordrawable", version = "1.1.0" }
    androidx-versionedparcelable-versionedparcelable-1-1-1 = { module = "androidx.versionedparcelable:versionedparcelable", version = "1.1.1" }
    androidx-viewpager-viewpager-1-0-0 = { module = "androidx.viewpager:viewpager", version = "1.0.0" }
    com-google-guava-listenablefuture-1-0 = { module = "com.google.guava:listenablefuture", version = "1.0" }
    junit-junit-4-12 = { module = "junit:junit", version = "4.12" }
    org-hamcrest-hamcrest-core-1-3 = { module = "org.hamcrest:hamcrest-core", version = "1.3" }
    org-jetbrains-kotlin-kotlin-stdlib-common-1-8-22 = { module = "org.jetbrains.kotlin:kotlin-stdlib-common", version = "1.8.22" }
    org-jetbrains-kotlin-kotlin-stdlib-jdk7-1-6-21 = { module = "org.jetbrains.kotlin:kotlin-stdlib-jdk7", version = "1.6.21" }
    org-jetbrains-kotlin-kotlin-stdlib-jdk8-1-6-21 = { module = "org.jetbrains.kotlin:kotlin-stdlib-jdk8", version = "1.6.21" }
    org-jetbrains-kotlin-kotlin-stdlib-1-8-22 = { module = "org.jetbrains.kotlin:kotlin-stdlib", version = "1.8.22" }
    org-jetbrains-kotlinx-kotlinx-coroutines-android-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version = "1.6.4" }
    org-jetbrains-kotlinx-kotlinx-coroutines-core-jvm-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm", version = "1.6.4" }
    org-jetbrains-kotlinx-kotlinx-coroutines-core-1-6-4 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.6.4" }
    org-jetbrains-annotations-13-0 = { module = "org.jetbrains:annotations", version = "13.0" }
    '''.stripIndent()

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
