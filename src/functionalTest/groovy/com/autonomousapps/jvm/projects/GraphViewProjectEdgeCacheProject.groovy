// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.SettingsScript
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

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
        s.sources = CONSUMER_SOURCE
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies(implementation(':direct'))
          bs.withGroovy("""\
          if (providers.systemProperty('edge').present) {
            sourceSets.main.kotlin.srcDir('src/edge/kotlin')
          }""".stripIndent())
        }
      }
      .withSubproject('direct') { s ->
        s.sources = DIRECT_SOURCE
        s.withBuildScript { bs ->
          bs.plugins = kotlin
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
        s.sources = TRANSITIVE_SOURCE
        s.withBuildScript { bs ->
          bs.plugins = kotlin
        }
      }
      .write()
  }

  private static final List<Source> CONSUMER_SOURCE = [
    Source.kotlin(
      '''\
      package com.example

      import com.example.direct.Direct
      
      class Main {
        private val direct = Direct()
      }'''.stripIndent()
    ).build(),
    Source.kotlin(
      '''\
      package com.example

      import com.example.transitive.Transitive
      
      class UsesTransitive {
        private val transitive = Transitive()
      }'''.stripIndent()
    )
      .withSourceSet('edge')
      .build(),
  ]

  private static final List<Source> DIRECT_SOURCE = [
    Source.kotlin(
      '''\
      package com.example.direct
      
      class Direct'''.stripIndent()
    ).build(),
  ]

  private static final List<Source> TRANSITIVE_SOURCE = [
    Source.kotlin(
      '''\
      package com.example.transitive
      
      class Transitive'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(projectCoordinates(':transitive'), 'implementation')
  ]

  private static Set<Advice> directAdvice = [
    Advice.ofRemove(projectCoordinates(':transitive'), 'api')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':direct', directAdvice),
    emptyProjectAdviceFor(':transitive'),
  ]
}
