// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.gradle.dependencies.PluginProvider
import com.autonomousapps.kit.gradle.dependencies.Plugins

abstract class AbstractAndroidProject extends AbstractProject {

  private static final DEFAULT_APP_NAMESPACE = 'com.example'
  private static final DEFAULT_LIB_NAMESPACE = 'com.example.lib'
  private static final DEFAULT_TEST_NAMESPACE = 'com.example.test'

  protected final androidAppPlugin = [Plugins.androidApp, Plugins.dependencyAnalysisNoVersion]
  protected final androidLibPlugin = [Plugins.androidLib, Plugins.dependencyAnalysisNoVersion]
  protected final androidAppWithKotlin = [Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]
  protected final androidLibWithKotlin = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion]

  protected final String agpVersion
  protected final AgpVersion version

  AbstractAndroidProject(String agpVersion) {
    super(getKotlinVersion(), agpVersion)

    this.agpVersion = agpVersion
    this.version = AgpVersion.version(agpVersion)
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

  protected AndroidBlock defaultAndroidTestBlock(
    String targetProjectPath,
    boolean withKotlin = true,
    String namespace = DEFAULT_TEST_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidTestBlock(targetProjectPath, withKotlin, defaultLibNamespace(namespace))
  }

  protected AndroidManifest appManifest(String namespace = DEFAULT_APP_NAMESPACE) {
    return AndroidManifest.appWithoutPackage(namespace)
  }

  protected AndroidManifest libraryManifest(String namespace = DEFAULT_LIB_NAMESPACE) {
    return null
  }

  private String defaultAppNamespace(String namespace) {
    return namespace
  }

  private String defaultLibNamespace(String namespace) {
    return namespace
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
