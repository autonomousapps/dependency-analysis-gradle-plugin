// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.utils.DebugAware

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject extends AbstractGradleProject {

  private static final String PRINT_ADVICE = "dependency.analysis.print.build.health=true"
  protected static final GradleProperties ADDITIONAL_PROPERTIES = GradleProperties.of(PRINT_ADVICE)

  /** Applies the 'org.jetbrains.kotlin.jvm' plugin. */
  protected static final List<Plugin> kotlinOnly = [Plugins.kotlinJvmNoVersion]

  /** Applies the 'org.jetbrains.kotlin.jvm' and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> kotlin = [Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion]

  /** Applies the 'java-library' and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> javaLibrary = [Plugin.javaLibrary, Plugins.dependencyAnalysisNoVersion]

  @Override
  protected GradleProject.Builder newGradleProjectBuilder(
    GradleProject.DslKind dslKind = GradleProject.DslKind.GROOVY
  ) {
    def additionalProperties = ADDITIONAL_PROPERTIES
    // There is a Gradle bug that makes tests break when the test uses CC/IP and we're also debugging
    if (!DebugAware.debug) {
      additionalProperties += GradleProperties.enableConfigurationCache()
    }

    return super.newGradleProjectBuilder(dslKind)
      .withRootProject { r ->
        r.gradleProperties += additionalProperties
        r.withBuildScript { bs ->
          bs.plugins(Plugins.dependencyAnalysis, Plugins.kotlinJvmNoApply)
        }
      }
  }

  protected GradleProject.Builder newSettingsProjectBuilder(
    map = [:]
  ) {
    def dslKind = map['dslKind'] ?: GradleProject.DslKind.GROOVY
    def withKotlin = map['withKotlin'] ?: false

    //noinspection GroovyAssignabilityCheck
    return newSettingsProjectBuilder(dslKind, withKotlin)
  }

  protected GradleProject.Builder newSettingsProjectBuilder(
    GradleProject.DslKind dslKind,
    boolean withKotlin
  ) {
    def additionalProperties = ADDITIONAL_PROPERTIES
    // There is a Gradle bug that makes tests break when the test uses CC/IP and we're also debugging
    if (!DebugAware.debug) {
      additionalProperties += GradleProperties.enableConfigurationCache()
    }

    def plugins = [Plugins.buildHealth]
    if (withKotlin) {
      plugins.add(Plugins.kotlinJvmNoApply)
    }

    return super.newGradleProjectBuilder(dslKind)
      .withRootProject { r ->
        r.gradleProperties += additionalProperties
        r.withSettingsScript { s ->
          s.plugins(plugins)
        }
      }
  }
}
