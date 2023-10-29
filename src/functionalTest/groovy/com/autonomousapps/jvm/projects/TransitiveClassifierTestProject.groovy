package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class TransitiveClassifierTestProject extends AbstractProject {

  enum TestProjectVariant {
    DEPENDENCY_WITH_CLASSIFIER_EXISTS,
    ADVICE_DEPENDENCY_WITH_CLASSIFIER,
    ADVICE_DEPENDENCY_WITH_CAPABILITY
  }

  static fixJodaTimeVariantsMetadataRule = '''
    dependencies.components.withModule('joda-time:joda-time') {
        addVariant('noTzdbCompile', 'compile') {
            withCapabilities {
                removeCapability('joda-time', 'joda-time')
                addCapability('joda-time', 'joda-time-no-tzdb', id.version)
            }
            withFiles {
                removeAllFiles()
                addFile("joda-time-${id.version}-no-tzdb.jar")
            }
        }
        addVariant('noTzdbRuntime', 'runtime') {
            withCapabilities {
                removeCapability('joda-time', 'joda-time')
                addCapability('joda-time', 'joda-time-no-tzdb', id.version)
            }
            withFiles {
                removeAllFiles()
                addFile("joda-time-${id.version}-no-tzdb.jar")
            }
        }
    }'''.stripIndent()

  final TestProjectVariant variant
  final GradleProject gradleProject

  TransitiveClassifierTestProject(TestProjectVariant variant) {
    this.variant = variant
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.additions = fixJodaTimeVariantsMetadataRule
        switch (variant) {
          case TestProjectVariant.DEPENDENCY_WITH_CLASSIFIER_EXISTS:
            bs.dependencies = [
              androidJoda('implementation'),
              jodaTimeNoTzdbClassifier('implementation')
            ]
            break
          case TestProjectVariant.ADVICE_DEPENDENCY_WITH_CLASSIFIER:
            bs.dependencies = [
              androidJoda('implementation')
            ]
            break
          case TestProjectVariant.ADVICE_DEPENDENCY_WITH_CAPABILITY:
            bs.dependencies = [
              // Use the local 'android.joda' that has a proper dependency with capability to 'joda-time-no-tzdb'
              project('implementation', ':android.joda')
            ]
            break
        }
      }
    }
    builder.withSubproject('android.joda') { s ->
      s.sources = androidJodaSources
      s.withBuildScript { bs ->
        bs.withGroovy('group = "local.test"\n' + fixJodaTimeVariantsMetadataRule)
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          jodaTimeNoTzdbFeature('api')
        ]
      }
    }

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
        }""".stripIndent()
    )
  ]

  private androidJodaSources = [
    new Source(
      SourceType.JAVA, "DateUtils", "com/example/android/joda",
      """\
        package com.example.consumer.test;

        import org.joda.time.DateMidnight;

        public class DateUtils {
          public DateMidnight midnight;
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedConsumerAdvice() {
    switch (variant) {
      case TestProjectVariant.DEPENDENCY_WITH_CLASSIFIER_EXISTS:
        return [Advice.ofRemove(moduleCoordinates(androidJoda('')), 'implementation')]
      case TestProjectVariant.ADVICE_DEPENDENCY_WITH_CLASSIFIER:
        return [Advice.ofRemove(moduleCoordinates(androidJoda('')), 'implementation'),
                // TODO https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/342
                //      Currently, we suggest adding the dependency without the classifier, as there is no way we
                //      can say with certainty that a classifier is needed.
                Advice.ofAdd(moduleCoordinates('joda-time:joda-time', '2.10.7'), 'implementation')]
      case TestProjectVariant.ADVICE_DEPENDENCY_WITH_CAPABILITY:
        return [Advice.ofRemove(projectCoordinates(':android.joda'), 'implementation'),
                Advice.ofAdd(moduleCoordinates('joda-time:joda-time', '2.10.7', 'joda-time:joda-time-no-tzdb'),
                  'implementation')]
    }
    return []
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    [
      emptyProjectAdviceFor(':android.joda'),
      projectAdviceForDependencies(':consumer', expectedConsumerAdvice())
    ]
  }
}
