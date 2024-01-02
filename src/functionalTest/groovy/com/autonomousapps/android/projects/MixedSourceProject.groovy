// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class MixedSourceProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  MixedSourceProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer', 'com.example.consumer') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib]
          bs.android = defaultAndroidLibBlock(false, 'com.example.consumer')
          bs.dependencies = [
            project('implementation', ':lib'),
          ]
        }
        lib.sources = consumerSources
      }
      .withSubproject('lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
        }
        lib.sources = libSources
      }.write()
  }

  private consumerSources = [
    new Source(
      SourceType.JAVA, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer;
        
        import com.example.lib.Lib;
        
        public class Consumer {
                  
          private void magic() {
            new Lib();
          }
        }
      """.stripIndent()
    )
  ]

  private libSources = [
    new Source(
      SourceType.JAVA, 'Lib', 'com/example/lib',
      """\
        package com.example.lib;
        
        // used by :app
        public class Lib {}
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'LibK', 'com/example/lib',
      """\
        package com.example.lib
        
        // unused by :app
        class LibK
      """.stripIndent(),
      Source.DEFAULT_SOURCE_SET,
      'java'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':lib'),
  ]
}
