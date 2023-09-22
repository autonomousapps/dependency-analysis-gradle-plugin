package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

abstract class SourceSetFilteringProject extends AbstractProject {

  enum Severity {
    FAIL("fail"),
    WARN("warn"),
    IGNORE("ignore");

    final String value

    Severity(String value) {
      this.value = value
    }
  }

  protected final Severity severity

  SourceSetFilteringProject(Severity severity) {
    this.severity = severity
  }

  protected abstract GradleProject build()

  /**
   * When {@code severity} is {@code IGNORE}, then the advice is empty and the build succeeds. When it is {@code WARN},
   * advice is not empty, and furthermore build continues to succeed.
   */
  static final class Filtering extends SourceSetFilteringProject {

    final GradleProject gradleProject

    Filtering(Severity severity) {
      super(severity)
      gradleProject = build()
    }

    @Override protected GradleProject build() {
      def builder = newGradleProjectBuilder()

      builder.withRootProject { root ->
        root.withBuildScript { bs ->
          bs.additions = """\
          dependencyAnalysis {
            issues {
              all {
                onIncorrectConfiguration {
                  severity "fail"
                }
                sourceSet("extraFeature") {
                  onIncorrectConfiguration {
                    severity "${severity.value}"
                  }
                }
              }
            }
          }
        """.stripIndent()
        }
      }

      builder.withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibraryPlugin]
          bs.featureVariants = ['extraFeature']
          bs.dependencies = [
            commonsCollections('api'),
            commonsCollections('extraFeatureApi')
          ]
          bs.additions = 'group = "examplegroup"'
        }
      }

      def project = builder.build()
      project.writer().write()
      return project
    }

    private sources = [
      new Source(
        SourceType.JAVA, "Example", "com/example",
        """\
        package com.example;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class Example {
          public HashBag<String> bag;
        }
      """.stripIndent()
      ),
      new Source(
        SourceType.JAVA, "ExtraFeature", "com/example/extra",
        """\
        package com.example.extra;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExtraFeature {
          private HashBag<String> internalBag;
        }
      """.stripIndent(),
        "extraFeature"
      )
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> expectedProjAdvice = [
      Advice.ofChange(
        moduleCoordinates(commonsCollections('')),
        'extraFeatureApi',
        'extraFeatureImplementation'
      )
    ]

    final Set<ProjectAdvice> expectedBuildHealth() {
      [
        projectAdviceForDependencies(
          ':proj',
          severity == Severity.WARN ? expectedProjAdvice : [] as Set<Advice>
        )
      ]
    }
  }

  /**
   * TODO.
   */
  static final class Layering extends SourceSetFilteringProject {

    // should be api
    protected static final commonsCollections = commonsCollections('extraFeatureImplementation')
    // unused, but we do use okio transitively
    protected static final okHttp = okHttp('extraFeatureImplementation')
    // undeclared but used via okhttp
    protected static final okio = okio('extraFeatureApi')

    final GradleProject gradleProject

    Layering(Severity severity) {
      super(severity)
      gradleProject = build()
    }

    @Override protected GradleProject build() {
      def builder = newGradleProjectBuilder()

      builder.withRootProject { root ->
        root.withBuildScript { bs ->
          bs.additions = """\
          dependencyAnalysis {
            issues {
              all {
                onAny {
                  severity "fail"
                }
                sourceSet("extraFeature") {
                  onUsedTransitiveDependencies {
                    severity "${severity.value}"
                  }
                  onIncorrectConfiguration {
                    severity "${severity.value}"
                  }
                }
              }
            }
          }
        """.stripIndent()
        }
      }

      builder.withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibraryPlugin]
          bs.featureVariants = ['extraFeature']
          bs.dependencies = [
            commonsCollections,
            okHttp
          ]
          bs.additions = 'group = "examplegroup"'
        }
      }

      def project = builder.build()
      project.writer().write()
      return project
    }

    private sources = [
      new Source(
        SourceType.JAVA, "ExtraFeature", "com/example/extra",
        """\
        package com.example.extra;
        
        import okio.Buffer;
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExtraFeature {
          public Buffer buffer;
          public HashBag<String> bag;
        }
      """.stripIndent(),
        "extraFeature"
      )
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final removeOkHttp = Advice.ofRemove(moduleCoordinates(okHttp), okHttp.configuration)

    private final Set<Advice> expectedFullAdvice = [
      removeOkHttp,
      Advice.ofChange(
        moduleCoordinates(commonsCollections),
        commonsCollections.configuration,
        'extraFeatureApi'
      ),
      Advice.ofAdd(moduleCoordinates(okio), 'extraFeatureApi')
    ]

    private final Set<Advice> expectedLayeredAdvice = [
      removeOkHttp
    ]

    final Set<ProjectAdvice> expectedBuildHealth() {
      [
        projectAdviceForDependencies(
          ':proj',
          severity == Severity.FAIL ? expectedFullAdvice : expectedLayeredAdvice,
          true
        )
      ]
    }
  }
}
