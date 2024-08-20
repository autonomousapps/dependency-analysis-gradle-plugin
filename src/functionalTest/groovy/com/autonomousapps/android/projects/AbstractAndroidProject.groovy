// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.gradle.dependencies.Plugins

abstract class AbstractAndroidProject extends AbstractProject {

  private static final AGP_8_0 = AgpVersion.version('8.0')
  private static final DEFAULT_APP_NAMESPACE = 'com.example'
  private static final DEFAULT_LIB_NAMESPACE = 'com.example.lib'

  protected final androidAppPlugin = [Plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
  protected final androidLibPlugin = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
  protected final androidAppWithKotlin = [Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
  protected final androidLibWithKotlin = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]

  protected final String agpVersion
  protected final AgpVersion version

  AbstractAndroidProject(String agpVersion) {
    this.agpVersion = agpVersion
    version = AgpVersion.version(agpVersion)
  }

  protected AndroidBlock defaultAndroidAppBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_APP_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidAppBlock(withKotlin, defaultAppNamespace(namespace))
  }

  protected AndroidBlock defaultAndroidLibBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_LIB_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidLibBlock(withKotlin, defaultLibNamespace(namespace))
  }

  protected AndroidManifest appManifest(String namespace = DEFAULT_APP_NAMESPACE) {
    return version >= AGP_8_0 ? AndroidManifest.appWithoutPackage(namespace) : AndroidManifest.app(namespace)
  }

  protected AndroidManifest libraryManifest(String namespace = DEFAULT_LIB_NAMESPACE) {
    return version >= AGP_8_0 ? null : AndroidManifest.defaultLib(namespace)
  }

  private String defaultAppNamespace(String namespace) {
    return version >= AGP_8_0 ? namespace : null
  }

  private String defaultLibNamespace(String namespace) {
    return version >= AGP_8_0 ? namespace : null
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

  protected GradleProject.Builder newAndroidSettingsProjectBuilder(
    map = [:]
  ) {
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
