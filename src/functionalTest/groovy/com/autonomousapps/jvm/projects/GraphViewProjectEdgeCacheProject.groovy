// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.SettingsScript

final class GraphViewProjectEdgeCacheProject extends AbstractProject {

  final GradleProject gradleProject

  GraphViewProjectEdgeCacheProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { s ->
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
      .withSubproject('consumer') { s ->
        s.sources = [CONSUMER_SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.withGroovy("""\
          dependencies {
            implementation project(':middle')
          }""")
        }
      }
      .withSubproject('middle') { s ->
        s.sources = [MIDDLE_SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = kotlinOnly
          // The system property models an upstream change adding a project edge: the
          // consumer's own build script and declarations are untouched by it.
          bs.withGroovy("""\
          if (providers.systemProperty('edge').present) {
            dependencies {
              api project(':leaf')
            }
          }""")
        }
      }
      .withSubproject('leaf') { s ->
        s.sources = [LEAF_SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = kotlinOnly
        }
      }
      .write()
  }

  private static final Source CONSUMER_SOURCE = new Source(
    SourceType.KOTLIN, 'Main', 'com/example',
    """\
      package com.example
      
      class Main""".stripIndent()
  )

  private static final Source MIDDLE_SOURCE = new Source(
    SourceType.KOTLIN, 'Middle', 'com/example/middle',
    """\
      package com.example.middle
      
      class Middle""".stripIndent()
  )

  private static final Source LEAF_SOURCE = new Source(
    SourceType.KOTLIN, 'Leaf', 'com/example/leaf',
    """\
      package com.example.leaf
      
      class Leaf""".stripIndent()
  )
}
