package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class DuplicateClasspathProject extends AbstractProject {

  final GradleProject gradleProject

  DuplicateClasspathProject(String filter = null, String severity = null) {
    this.gradleProject = build(filter, severity)
  }

  private GradleProject build(String filter, String severity) {
    def configuration = new DagpConfiguration(filter, severity).toString()

    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy(configuration)
        }
      }
    // :consumer uses the Producer class.
    // This class is provided by both
    // 1. :producer-2, which is a direct dependency, and
    // 2. :producer-1, which is a transitive dependency (of :unused)
    // These classes have incompatible definitions. :consumer _requires_ the version provided by :producer-2.
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + plugins.gradleDependenciesSorter
          bs.dependencies(
            project('implementation', ':unused'),
            project('implementation', ':producer-2'),
          )

          // We need to sort the dependencies after rewriting so that the classpath loads the class with the
          // incompatible definition, causing `:consumer:compileJava` to fail.
          bs.withGroovy(
            '''\
              tasks.named { it == 'fixDependencies' }.configureEach {
                finalizedBy('sortDependencies')
              }
            '''
          )
        }
      }
    // Used
      .withSubproject('producer-1') { s ->
        s.sources = sourcesProducer1
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
    // Unused, except its transitive is
      .withSubproject('unused') { s ->
        s.sources = sourcesUnused
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            project('api', ':producer-1'),
          )
        }
      }
    // Used?
      .withSubproject('producer-2') { s ->
        s.sources = sourcesProducer2
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final List<Source> sourcesConsumer = [
    Source.java(
      '''\
        package com.example.consumer;

        import com.example.producer.Producer;
        import com.example.producer.Producer.Inner;

        public class Consumer {
          private Producer producer = new Producer("Emma", "Goldman");
          private Producer.Inner inner;
        }
      '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  private static final List<Source> sourcesProducer1 = [
    Source.java(
      '''\
        package com.example.producer;

        public class Producer {
          
          private final String name;
          
          public Producer(String name) {
            this.name = name;
          }
          
          public static class Inner {}
        }
      '''
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
  ]

  // Same class file as sourcesProducer1, incompatible definition
  private static final List<Source> sourcesProducer2 = [
    Source.java(
      '''\
        package com.example.producer;

        public class Producer {
          private final String firstName;
          private final String lastName;
          
          public Producer(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
          }
          
          public static class Inner {}
          
          // This is here only for manually inspecting asm.kt log output
          public class NonStatic {
            String name = Producer.this.firstName;
          }
        }
      '''
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
  ]

  private static final List<Source> sourcesUnused = [
    Source.java(
      '''\
        package com.example.unused;

        import com.example.producer.Producer;

        public class Unused {
          public Producer producer;
        }
      '''
    )
      .withPath('com.example.unused', 'Unused')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation'),
    // This is actually bad advice, but we can't detect it without re-reverting the change to detect binary
    // incompatibilities.
    Advice.ofAdd(projectCoordinates(':producer-1'), 'implementation')
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice, true),
    emptyProjectAdviceFor(':unused'),
    emptyProjectAdviceFor(':producer-1'),
    emptyProjectAdviceFor(':producer-2'),
  ]

  static class DagpConfiguration {

    private final String filter
    private final String severity

    DagpConfiguration(String filter, String severity) {
      this.filter = filter
      this.severity = severity
    }

    @Override
    String toString() {
      def builder = new StringBuilder()
      builder.append('dependencyAnalysis {\n')
      builder.append('  issues {\n')
      builder.append('    all {\n')
      builder.append('      onAny {\n')
      builder.append('        severity \'fail\'\n')
      builder.append('      }\n')

      if (filter || severity) {
        builder.append('      onDuplicateClassWarnings {\n')
        if (severity) {
          builder.append("        severity \'$severity\'\n")
        }
        if (filter) {
          builder.append("        exclude \'$filter\'\n")
        }
        builder.append('      }\n')
      }

      builder.append('    }\n')
      builder.append('  }\n')
      builder.append('}')

      return builder.toString()
    }
  }
}
