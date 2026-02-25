// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.DependencyProvider
import com.autonomousapps.kit.gradle.dependencies.PluginProvider
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

  /** Applies the 'java', 'application', and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> javaApp = [Plugin.java, Plugin.application, Plugins.dependencyAnalysisNoVersion]

  protected static final Plugin javaTestFixtures = Plugins.javaTestFixtures

  /** Applies the 'org.jetbrains.kotlin.multiplatform' and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> kmpLibrary = [Plugins.kotlinMultiplatformNoVersion, Plugins.dependencyAnalysisNoVersion]

  /**
   * Applies the 'org.jetbrains.kotlin.multiplatform', 'com.android.kotlin.multiplatform.library' and
   * 'com.autonomousapps.dependency-analysis' plugins.
   *
   * @see <a href="https://developer.android.com/kotlin/multiplatform/plugin#apply">Apply the Android-KMP plugin to an existing module</a>
   */
  protected static final List<Plugin> androidKmpLibrary = kmpLibrary + [Plugins.androidKmpLibNoVersion]

  protected final DependencyProvider dependencies
  protected final PluginProvider plugins

  static String getKotlinVersion() {
    return System.getProperty("com.autonomousapps.test.versions.kotlin")
  }

  /**
   * In some (but not all) cases, test fixtures must be built with a different (later) version of Kotlin to avoid
   * errors <a href="https://scans.gradle.com/s/fi4p2cqkohb24/tests/task/:functionalTest/details/com.autonomousapps.android.ResSpec/gracefully%20handles%20dataBinding%20expressions%20in%20res%20files%20(Gradle%209.0.0%20AGP%208.13.0)?top-execution=1">like this:</a>
   *
   * <pre>
   * e: file:///home/runner/work/dependency-analysis-gradle-plugin/dependency-analysis-gradle-plugin/build/tmp/functionalTest/work/.gradle-test-kit/caches/9.0.0/transforms/abc6055f6fb5acdd209c94b73930a5e2/transformed/databinding-ktx-8.13.0-api.jar!/META-INF/databindingKtx_release.kotlin_module
   *  Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0, expected version is 2.0.0.
   * </pre>
   */
  static String getLaterKotlinVersion() {
    return System.getProperty('com.autonomousapps.test.versions.kotlin.later')
  }

  AbstractProject() {
    this(getKotlinVersion(), null)
  }

  AbstractProject(String kotlinVersion) {
    this(kotlinVersion, null)
  }

  AbstractProject(
    String kotlinVersion,
    String agpVersion
  ) {
    dependencies = new DependencyProvider(kotlinVersion)
    plugins = new PluginProvider(
      kotlinVersion,
      agpVersion,
    )
  }

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
          bs.plugins(plugins.dependencyAnalysis, plugins.kotlinJvmNoApply)
        }
      }
  }

  protected GradleProject.Builder newSettingsProjectBuilder(map = [:]) {
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

    def appliedPlugins = [plugins.buildHealth]
    if (withKotlin) {
      appliedPlugins.add(plugins.kotlinJvmNoApply)
    }

    return super.newGradleProjectBuilder(dslKind)
      .withRootProject { r ->
        r.gradleProperties += additionalProperties
        r.withSettingsScript { s ->
          s.plugins(appliedPlugins)
        }
      }
  }
}
