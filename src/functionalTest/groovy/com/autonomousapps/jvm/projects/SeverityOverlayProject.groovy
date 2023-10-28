package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.okHttp

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
    def builder = newGradleProjectBuilder()
    builder.withRootProject { s ->
      s.withBuildScript { bs ->
        bs.additions = """\
          dependencyAnalysis {
            issues {
              all {
                onUsedTransitiveDependencies {
                  severity '${rootSeverity()}'
                }
              }
            }
          }""".stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [okHttp('implementation')]
        bs.additions = """\
          dependencyAnalysis {
            issues {
              onUsedTransitiveDependencies {
                ${projSeverity()}
              }
            }
          }""".stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
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
