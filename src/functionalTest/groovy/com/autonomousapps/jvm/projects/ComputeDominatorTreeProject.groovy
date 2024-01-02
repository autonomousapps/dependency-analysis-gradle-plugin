// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin

import static com.autonomousapps.kit.gradle.Dependency.implementation

final class ComputeDominatorTreeProject extends AbstractProject {

  final GradleProject gradleProject

  ComputeDominatorTreeProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject("app") { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary]
          bs.dependencies = [
            implementation('com.squareup.misk:misk:2023.10.18.080259-adcfb84')
          ]
        }
      }
      .write()
  }

  String actualCompileDominatorTree() {
    return gradleProject.rootDir.toPath()
      .resolve('app/build/reports/dependency-analysis/main/graph/graph-dominator.txt')
      .readLines()
  }

  String expectedCompileDominatorTreeTotalSize() {
    return "18.43 MiB :app"
  }

  String actualRuntimeDominatorTree() {
    return gradleProject.rootDir.toPath()
      .resolve('app/build/reports/dependency-analysis/main/graph/graph-dominator-runtime.txt')
      .readLines()
  }

  String expectedRuntimeDominatorTreeTotalSize() {
    return "61.49 MiB :app"
  }
}
