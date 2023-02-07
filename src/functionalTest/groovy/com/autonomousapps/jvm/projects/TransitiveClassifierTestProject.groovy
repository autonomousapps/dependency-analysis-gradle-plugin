package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.androidJoda
import static com.autonomousapps.kit.Dependency.jodaTimeNoTzdbClassifier

final class TransitiveClassifierTestProject extends AbstractProject {

  final boolean withDependencyWithCapability
  final GradleProject gradleProject

  TransitiveClassifierTestProject(boolean withDependencyWithClassifier) {
    this.withDependencyWithCapability = withDependencyWithClassifier
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = withDependencyWithCapability
        ? [
          androidJoda('implementation'),
          jodaTimeNoTzdbClassifier('implementation')
        ]
        : [
          androidJoda('implementation')
        ]
      }
    }
    // TODO Add subproject which mimics android.joda's transitive dependency setup but with 'requiresCapability'.
    //      Then add a another case to the test that shows that everything is correct if feature variants would be used.

    def project = builder.build()
    project.writer().write()
    return project
  }

  private consumerSources = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer.test;

        import org.joda.time.DateMidnight;

        public class Consumer {
          DateMidnight midnight;
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {[
    withDependencyWithCapability
    ? projectAdviceForDependencies(':consumer', [
      Advice.ofRemove(moduleCoordinates(androidJoda('')), 'implementation')
    ] as Set)
    : projectAdviceForDependencies(':consumer', [
      // TODO https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/342
      //      This suggests adding the dependency without the classifier, as there is not way we can say with
      //      certainty that a classifier is needed.
      Advice.ofAdd(moduleCoordinates('joda-time:joda-time', '2.10.7'), 'implementation'),
      Advice.ofRemove(moduleCoordinates(androidJoda('')), 'implementation')
    ] as Set)
  ]}
}
