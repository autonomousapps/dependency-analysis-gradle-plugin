// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency

final class NoAbsolutePathsProject extends AbstractProject {

  final GradleProject gradleProject

  NoAbsolutePathsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = [KOTLIN_SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            new Dependency('implementation', 'org.apache.commons:commons-lang3:3.14.0'),
          ]
        }
      }
      .write()
  }

  private static final Source KOTLIN_SOURCE = new Source(
    SourceType.KOTLIN, 'Main', 'com/example',
    """\
      package com.example

      import org.apache.commons.lang3.StringUtils

      class Main {
        fun greet(name: String): String = StringUtils.capitalize(name)
      }""".stripIndent()
  )
}
