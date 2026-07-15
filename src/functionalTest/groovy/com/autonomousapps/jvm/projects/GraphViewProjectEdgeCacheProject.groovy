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
        s.withFile(
          'src/edge/kotlin/com/example/UsesTransitive.kt',
          """\
          package com.example

          import com.example.transitive.Transitive

          class UsesTransitive {
            private val transitive = Transitive()
          }
          """.stripIndent()
        )
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.withGroovy("""\
          if (providers.systemProperty('edge').present) {
            sourceSets.main.kotlin.srcDir('src/edge/kotlin')
          }

          dependencies {
            implementation project(':direct')
          }""")
        }
      }
      .withSubproject('direct') { s ->
        s.sources = [DIRECT_SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = kotlinOnly
          // The system property models an upstream change adding a project edge: the
          // consumer's own build script and declarations are untouched by it.
          bs.withGroovy("""\
          if (providers.systemProperty('edge').present) {
            dependencies {
              api project(':transitive')
            }
          }""")
        }
      }
      .withSubproject('transitive') { s ->
        s.sources = [TRANSITIVE_SOURCE]
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

      import com.example.direct.Direct
      
      class Main {
        private val direct = Direct()
      }""".stripIndent()
  )

  private static final Source DIRECT_SOURCE = new Source(
    SourceType.KOTLIN, 'Direct', 'com/example/direct',
    """\
      package com.example.direct
      
      class Direct""".stripIndent()
  )

  private static final Source TRANSITIVE_SOURCE = new Source(
    SourceType.KOTLIN, 'Transitive', 'com/example/transitive',
    """\
      package com.example.transitive
      
      class Transitive""".stripIndent()
  )
}
