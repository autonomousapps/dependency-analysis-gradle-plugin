package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.*

final class GradleBuildSrcConventionMultiConfigProject extends AbstractProject {

  final GradleProject gradleProject

  GradleBuildSrcConventionMultiConfigProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withBuildSrc { s ->
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.groovyGradlePlugin]
        bs.repositories = [Repository.MAVEN_LOCAL, Repository.MAVEN_CENTRAL]
        bs.dependencies = [dagp('implementation')]
      }
      s.sources = buildSrcSources()
    }
    builder.withRootProject { s ->
      s.withBuildScript { bs ->
        bs.plugins = [new Plugin('com.autonomousapps.dependency-analysis-root-convention')]
      }
    }
    builder.withSubproject('proj-a') { s ->
      s.sources = []
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, new Plugin('com.autonomousapps.dependency-analysis-project-convention')]
        bs.dependencies = [
          new Dependency('implementation', 'gradleApi()'),
          project('implementation', ':proj-b')
        ]
        bs.additions = """\
          dependencyAnalysis {
              issues {
                // For some weird reason we still want to keep this dependency
                onUnusedDependencies {
                  exclude(
                    // bla bla some reason to keep this dependency
                    ':proj-b',
                  )
                }
              }
          }
        """.stripIndent()
      }
    }
    builder.withSubproject('proj-b') { s ->
      s.sources = [JAVA_SOURCE]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, new Plugin('com.autonomousapps.dependency-analysis-project-convention')]
        bs.dependencies = [commonsMath('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source JAVA_SOURCE = new Source(
    SourceType.JAVA, "Main", "com/example",
    """\
      package com.example;
      
      public class Main {
        public static void main(String... args) {
        }
      }
     """.stripIndent()
  )

  private static final Source[] buildSrcSources() {
    return [
      new Source(
        SourceType.GROOVY, "com.autonomousapps.dependency-analysis-root-convention", "",
        """\
          plugins {
              id 'com.autonomousapps.dependency-analysis'
          }
  
          dependencyAnalysis {
              issues {
                  all {
                      onAny {
                          severity('ignore')
                      }
                      onUnusedDependencies {
                        exclude(
                          // Common util dependencies are always applied, don't fail on these
                          'org.apache.commons:commons-math3',
                        )
                      }
                  }
              }
          }
       """.stripIndent()
      ),
      new Source(
        SourceType.GROOVY, "com.autonomousapps.dependency-analysis-project-convention", "",
        """\
          project.getPluginManager().withPlugin("com.autonomousapps.dependency-analysis", { plugin ->
              dependencyAnalysis {
                  issues {
                      onAny {
                          severity('fail')
                      }
                  }
              }
          })
       """.stripIndent()
      )
    ]
  }


  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    emptyCompAdviceFor(':proj-a'),
    emptyCompAdviceFor(':proj-b'),
  ]
}
