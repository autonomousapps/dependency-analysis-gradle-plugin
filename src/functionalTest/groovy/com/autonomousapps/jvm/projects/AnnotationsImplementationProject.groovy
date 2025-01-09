package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

final class AnnotationsImplementationProject extends AbstractProject {

  final GradleProject gradleProject

  AnnotationsImplementationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            project('testImplementation', ':testrunner'),
            junit('testImplementation'),
          )
        }
      }
      .withSubproject('testrunner') { s ->
        s.sources = SOURCE_TEST_RUNNER
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            junit('api'),
          )
        }
      }
      .write()
  }

  private static final Source SOURCE_CONSUMER = Source.java(
    '''\
      package com.example.test;
      
      import org.junit.runner.RunWith;

      @RunWith(MyTestRunner.class)
      public class TestSuite {}
    '''
  )
    .withPath('com.example.consumer', 'TestSuite')
    .withSourceSet('test')
    .build()

  private static final List<Source> SOURCE_TEST_RUNNER = [
    Source.java(
      '''\
        // Deliberate in the same package as the consumer, to avoid import-based detection heuristics
        package com.example.test;

        import org.junit.runner.Description;
        import org.junit.runner.Runner;
        import org.junit.runner.notification.RunNotifier;

        public class MyTestRunner extends Runner {
            public Description getDescription() {
              throw new IllegalStateException("not implemented");
            }

            public void run(RunNotifier notifier) {}
        }
      '''
    )
      .withPath('com.example.test', 'MyTestRunner')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':testrunner'),
  ]
}
