package com.autonomousapps

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.dependencies.Plugins

@SuppressWarnings('GrMethodMayBeStatic')
abstract class AbstractProject extends AbstractGradleProject {

  @Override
  protected GradleProject.Builder newGradleProjectBuilder() {
    return super.newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinNoApply]
        }
      }
  }
}
