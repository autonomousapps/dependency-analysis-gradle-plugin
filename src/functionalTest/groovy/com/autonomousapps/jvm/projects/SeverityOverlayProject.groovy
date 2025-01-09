// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okHttp

final class SeverityOverlayProject extends AbstractProject {

  enum Severity {
    UPGRADE, DOWNGRADE, UNCHANGED
  }

  private final Severity severity
  final GradleProject gradleProject

  SeverityOverlayProject(Severity severity) {
    this.severity = severity
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { s ->
        s.withBuildScript { bs ->
          bs.withGroovy("""\
          dependencyAnalysis {
            issues {
              all {
                onUsedTransitiveDependencies {
                  severity '${rootSeverity()}'
                }
              }
            }
          }""")
        }
      }
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [okHttp('implementation')]
          bs.withGroovy("""\
          dependencyAnalysis {
            issues {
              onUsedTransitiveDependencies {
                ${projSeverity()}
              }
            }
          }""")
        }
      }
      .write()
  }

  private String rootSeverity() {
    if (severity == Severity.UPGRADE) {
      // For the upgrade case, the root script should be set to warn, and the proj script to fail
      'warn'
    } else {
      // For the downgrade or unchanged case, the root script should be set to fail, and the proj script to warn or nothing
      'fail'
    }
  }

  private String projSeverity() {
    if (severity == Severity.UPGRADE) {
      // For the upgrade case, the root script should be set to warn, and the proj script to fail
      "severity 'fail'"
    } else if (severity == Severity.DOWNGRADE) {
      // For the downgrade case, the root script should be set to fail, and the proj script to warn
      "severity 'warn'"
    } else {
      // nada
      ''
    }
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import okhttp3.OkHttpClient;
        import okio.Buffer;

        public class Main {
          public static void main(String... args) {
            // implementation
            new OkHttpClient.Builder().build();
            
            // transitive from OkHttp
            Buffer buffer = new Buffer();
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio:2.6.0'), 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(
      ':proj',
      projAdvice,
      severity == Severity.UPGRADE || severity == Severity.UNCHANGED
    )
  ]
}
