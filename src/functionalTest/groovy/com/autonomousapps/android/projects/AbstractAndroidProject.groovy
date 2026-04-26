// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.gradle.dependencies.Plugins

abstract class AbstractAndroidProject extends AbstractProject {

  private static final DEFAULT_APP_NAMESPACE = 'com.example'
  private static final DEFAULT_LIB_NAMESPACE = 'com.example.lib'
  private static final DEFAULT_TEST_NAMESPACE = 'com.example.test'

  protected final String agpVersion
  protected final AgpVersion version
  protected final boolean isLessThanAgp9 = AgpVersion.version(agpVersion) < AgpVersion.version('9.0.0')
  protected final boolean isAtLeastAgp9 = !isLessThanAgp9

  protected final Plugin rootKapt = new Plugin(Plugins.legacyKaptId, agpVersion, false)

  protected List<Plugin> androidApp(boolean withKotlin = true) {
    if (isAtLeastAgp9) {
      [Plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
    } else if (withKotlin) {
      [Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
    } else {
      [Plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
    }
  }

  protected List<Plugin> androidAppWithVersions(boolean withKotlin = true) {
    if (isAtLeastAgp9) {
      [plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
    } else if (withKotlin) {
      [plugins.androidApp, plugins.kotlinAndroid, Plugins.dependencyAnalysisNoVersion]
    } else {
      [plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
    }
  }

  protected List<Plugin> androidLib(boolean withKotlin = true) {
    if (isAtLeastAgp9) {
      [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
    } else if (withKotlin) {
      [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
    } else {
      [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
    }
  }

  protected List<Plugin> androidTest(boolean withKotlin = true) {
    if (isAtLeastAgp9) {
      [Plugins.androidTest, Plugins.dependencyAnalysisNoVersion]
    } else if (withKotlin) {
      [Plugins.androidTest, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
    } else {
      [Plugins.androidTest, Plugins.dependencyAnalysisNoVersion]
    }
  }

  protected Plugin kapt() {
    return isAtLeastAgp9 ? Plugins.androidLegacyKaptNoVersion : Plugins.kotlinKaptNoVersion
  }

  AbstractAndroidProject(String kotlinVersion, String agpVersion) {
    super(kotlinVersion, agpVersion)

    this.agpVersion = agpVersion
    this.version = AgpVersion.version(agpVersion)
  }

  AbstractAndroidProject(String agpVersion) {
    this(getKotlinVersion(), agpVersion)
  }

  protected AndroidBlock defaultAndroidAppBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_APP_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidAppBlock(withKotlin, namespace)
  }

  protected AndroidBlock defaultAndroidLibBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_LIB_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidLibBlock(withKotlin, namespace)
  }

  protected AndroidBlock defaultAndroidTestBlock(
    String targetProjectPath,
    boolean withKotlin = true,
    String namespace = DEFAULT_TEST_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidTestBlock(targetProjectPath, withKotlin, namespace)
  }

  protected AndroidManifest appManifest(String namespace = DEFAULT_APP_NAMESPACE) {
    return AndroidManifest.appWithoutPackage(namespace)
  }

  protected AndroidManifest libraryManifest(String namespace = DEFAULT_LIB_NAMESPACE) {
    return null
  }

  protected GradleProject.Builder newAndroidGradleProjectBuilder(String agpVersion) {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.gradleProperties += GradleProperties.minimalAndroidProperties()
        root.withBuildScript { bs ->
          bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        }
      }
  }

  protected GradleProject.Builder newAndroidSettingsProjectBuilder(map = [:]) {
    def agpVersion = map['agpVersion'] as String
    if (agpVersion == null) {
      throw new IllegalArgumentException("'agpVersion' expected.")
    }

    def dslKind = map['dslKind'] ?: GradleProject.DslKind.GROOVY
    def withKotlin = map['withKotlin'] ?: false

    //noinspection GroovyAssignabilityCheck
    return newSettingsProjectBuilder(dslKind, withKotlin)
      .withRootProject { root ->
        root.gradleProperties += GradleProperties.minimalAndroidProperties()
        root.withSettingsScript { s ->
          s.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        }
      }
  }
}
