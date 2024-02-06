// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject

import java.nio.file.Files

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okHttp
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okio

final class FindDeclarationsProject extends AbstractProject {

  final name = 'proj'
  final GradleProject gradleProject

  FindDeclarationsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject(name) { s ->
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            okHttp('implementation'),
            okio('implementation'),
          ]
        }
      }
      .write()
  }

  void mutateBuildScript() {
    def f = gradleProject.rootDir.toPath().resolve("$name/build.gradle")
    assert Files.exists(f)

    def lines = f.readLines()
    lines.removeIf {
      it.contains('okio')
    }

    f.write(lines.join('\n'))
  }
}
