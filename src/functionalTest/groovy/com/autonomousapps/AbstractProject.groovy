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

  private static final String NO_AUTO_APPLY = "dependency.analysis.autoapply=false"
  private static final String PRINT_ADVICE = "dependency.analysis.print.build.health=true"
  protected static final String ADDITIONAL_PROPERTIES = GradleProperties.of(PRINT_ADVICE, NO_AUTO_APPLY)

  /** Applies the 'org.jetbrains.kotlin.jvm' plugin. */
  protected static final List<Plugin> kotlinOnly = [Plugins.kotlinNoVersion]

  /** Applies the 'org.jetbrains.kotlin.jvm' and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> kotlin = [Plugins.kotlinNoVersion, Plugins.dependencyAnalysisNoVersion]

  /** Applies the 'java-library' and 'com.autonomousapps.dependency-analysis' plugins. */
  protected static final List<Plugin> javaLibrary = [Plugin.javaLibrary, Plugins.dependencyAnalysisNoVersion]

  @Override
  protected GradleProject.Builder newGradleProjectBuilder(
    GradleProject.DslKind dslKind = GradleProject.DslKind.GROOVY
  ) {
    def additionalProperties = ADDITIONAL_PROPERTIES
    // There is a Gradle bug that makes tests break when the test uses CC and we're also debugging
    if (!DebugAware.debug) {
      additionalProperties += GradleProperties.enableConfigurationCache()
    }

    return super.newGradleProjectBuilder(dslKind)
      .withRootProject { r ->
        r.gradleProperties += additionalProperties
        r.withBuildScript { bs ->
          bs.plugins(Plugins.dependencyAnalysis, Plugins.kotlinNoApply)
        }
      }
  }
}
