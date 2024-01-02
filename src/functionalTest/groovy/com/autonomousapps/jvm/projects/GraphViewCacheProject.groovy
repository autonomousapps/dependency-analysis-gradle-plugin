// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.SettingsScript
import com.autonomousapps.kit.gradle.dependencies.Plugins

final class GraphViewCacheProject extends AbstractProject {

  final GradleProject gradleProject

  GraphViewCacheProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { s ->
      s.settingsScript = new SettingsScript().tap {
        // Since this test exercises the build cache, we can't rely on the default location
        additions = """
          buildCache {
            local {
              directory = new File(rootDir, 'build-cache')
            }
          }""".stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE]
      s.withBuildScript { bs ->
        bs.plugins = [Plugins.kotlinNoVersion]
        bs.withGroovy("""\
          dependencies {
            implementation providers.systemProperty('v').map { v ->
              "com.freeletics.mad:state-machine-jvm:\$v"
            }
          }""")
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE = new Source(
    SourceType.KOTLIN, 'Main', 'com/example',
    """\
      package com.example
            
      class Main {}""".stripIndent()
  )
}
