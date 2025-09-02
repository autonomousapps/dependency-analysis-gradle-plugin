// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class FileMutationProject extends AbstractProject {

  private final String libName = 'lib'
  final GradleProject gradleProject

  FileMutationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject(libName) { s ->
        s.sources = libSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  void deleteSourceFile() {
    def sourcePath = gradleProject.subprojects.first().sources
      .find { s -> s.name == "Library2" }
      .relativeFilePath()
    gradleProject.projectDir(libName).resolve(sourcePath).toFile().delete()
  }

  private final List<Source> libSources = [
    Source.java(
      '''\
      package com.example;
        
      public class Library {}'''.stripIndent()
    ).build(),
    Source.java(
      '''\
      package com.example;
        
      public class Library2 {}'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(":$libName"),
  ]
}
