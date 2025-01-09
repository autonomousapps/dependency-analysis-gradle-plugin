package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject

import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.project

final class WorkPlanProject extends AbstractProject {

  final GradleProject gradleProject

  WorkPlanProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
    // alpha
      .withSubproject('alpha:app') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaApp + plugins.javaTestFixtures
          bs.dependencies = [
            project('implementation', ':alpha:lib'),
            project('implementation', ':beta:app').onTestFixtures(),
          ]
        }
      }
      .withSubproject('alpha:lib') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':alpha:core'),
          ]
        }
      }
      .withSubproject('alpha:core') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(implementation('com.squareup.misk:misk:2023.10.18.080259-adcfb84'))
        }
      }

    // beta
      .withSubproject('beta:app') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaApp + plugins.javaTestFixtures
          bs.dependencies = [
            project('implementation', ':beta:lib'),
          ]
        }
      }
      .withSubproject('beta:lib') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':beta:core'),
          ]
        }
      }
      .withSubproject('beta:core') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }

    // gamma
      .withSubproject('gamma:app') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaApp + plugins.javaTestFixtures
          bs.dependencies = [
            project('implementation', ':gamma:lib'),
          ]
        }
      }
      .withSubproject('gamma:lib') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':gamma:core'),
          ]
        }
      }
      .withSubproject('gamma:core') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }
}
