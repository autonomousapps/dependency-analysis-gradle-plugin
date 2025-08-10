// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Repository

import static com.autonomousapps.kit.gradle.Dependency.implementation

final class GuavaProject extends AbstractProject {

  final GradleProject gradleProject

  GuavaProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withBuildSrc { buildSrc ->
        buildSrc.withBuildScript { bs ->
          bs.plugins(plugins.javaLibrary)
          bs.repositories = Repository.DEFAULT
          bs.dependencies(implementation("com.google.guava:guava:32.0.0-jre"))
        }
      }
      .withSubproject('lib') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }
}
