// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.utils.DebugAware

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject extends AbstractGradleProject {

  protected static final String PRINT_ADVICE = "dependency.analysis.print.build.health=true"

  @Override
  protected GradleProject.Builder newGradleProjectBuilder(
    GradleProject.DslKind dslKind = GradleProject.DslKind.GROOVY
  ) {
    def additionalProperties = GradleProperties.of(PRINT_ADVICE)
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
