package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.slf4j
import static com.autonomousapps.kit.gradle.Dependency.slf4jTests

final class ClassifierTestProject extends AbstractProject {

  final String variantCode
  final boolean nothingUsed
  final GradleProject gradleProject

  enum TestProjectVariant {
    ONLY_MAIN_NEEDED,
    ONLY_CLASSIFIED_NEEDED,
    BOTH_NEEDED,
    NON_NEEDED
  }

  ClassifierTestProject(TestProjectVariant variant) {
    switch (variant) {
      case TestProjectVariant.ONLY_MAIN_NEEDED: variantCode = '''
          public org.slf4j.Logger testLogger;
        '''
        break
      case TestProjectVariant.ONLY_CLASSIFIED_NEEDED: variantCode ='''
          private org.slf4j.basicTests.BasicMarkerTest basicMarkerTest;
        '''
        break
      case TestProjectVariant.BOTH_NEEDED: variantCode ='''
          public org.slf4j.Logger testLogger;
          private org.slf4j.basicTests.BasicMarkerTest basicMarkerTest;
        '''
        break
      case TestProjectVariant.NON_NEEDED: variantCode = ''
        break
    }
    this.nothingUsed = variant == TestProjectVariant.NON_NEEDED
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('consumer') { s ->
      s.sources = consumerTestSources()
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          slf4j('testImplementation'),
          slf4jTests('testImplementation'),
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private consumerTestSources() {[
    new Source(
      SourceType.JAVA, "ConsumerTest", "com/example/consumer/test",
      """\
        package com.example.consumer.test;

        public class ConsumerTest {
          $variantCode
        }""".stripIndent(),
      "test"
    )
  ]}

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {[
    nothingUsed
      ? projectAdviceForDependencies(':consumer', [
        Advice.ofRemove(moduleCoordinates(slf4j('')), 'testImplementation'),
      ] as Set)
      : emptyProjectAdviceFor(':consumer')
  ]}
}
