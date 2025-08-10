// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin

import static com.autonomousapps.kit.gradle.Dependency.project

final class CategoryProject extends AbstractProject {

  final GradleProject gradleProject

  CategoryProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins.addAll(Plugin.javaLibrary, Plugin.jacocoReportAggregation)
          bs.withGroovy(
            '''\
            tasks.named('test', Test) {
              useJUnitPlatform()
            }
            '''
          )
          bs.dependencies(
            project('jacocoAggregation', ':project'),
          )
        }
      }
      .withSubproject('project') { p ->
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary + Plugin.jvmTestSuite
          bs.withGroovy(
            '''\
            tasks.named('test', Test) {
              useJUnitPlatform()
            }
            '''
          )
        }
      }
      .write()
  }
}
