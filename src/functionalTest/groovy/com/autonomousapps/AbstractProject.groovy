package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject extends AbstractGradleProject {

  protected static final PRINT_ADVICE = "dependency.analysis.print.build.health=true"

  @Override
  protected GradleProject.Builder newGradleProjectBuilder() {
    return super.newGradleProjectBuilder()
      .withRootProject { r ->
        r.gradleProperties += GradleProperties.enableConfigurationCache() + PRINT_ADVICE
        r.withBuildScript { bs ->
          bs.plugins(Plugins.dependencyAnalysis, Plugins.kotlinNoApply)
        }
      }
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
}
