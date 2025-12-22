package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.AdviceHelper.projectCoordinates
import static com.autonomousapps.kit.gradle.Dependency.project

final class DuplicateAnnotationClasspathProject extends AbstractProject {

  private final boolean shouldFail
  final GradleProject gradleProject

   DuplicateAnnotationClasspathProject(
    String onAnySeverity = 'fail',
    List<String> duplicateClassesFilter = null, String duplicateClassesSeverity = null
  ) {
    this.shouldFail = onAnySeverity == 'fail'
    this.gradleProject = build(onAnySeverity, duplicateClassesFilter, duplicateClassesSeverity)
  }

  private GradleProject build(String onAnySeverity, List<String> duplicateClassesFilter, String duplicateClassesSeverity) {
    def configuration = new DagpConfiguration(
      onAnySeverity,
      duplicateClassesFilter,
      duplicateClassesSeverity,
    ).toString()

    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy(configuration)
        }
      }
    // :consumer uses the Annotate class.
    // This class is provided by both
    // 1. :annotation-2, which is a direct dependency, and
    // 2. :annotation-1, which is a transitive dependency (of :unused)
    // These classes have incompatible definitions. :consumer _requires_ the version provided by :annotation-2.
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + plugins.gradleDependenciesSorter
          bs.dependencies(
            project('implementation', ':unused'),
            project('api', ':annotation-2'),
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
      .withSubproject('annotation-1') { s ->
        s.sources = sourcesAnnotation1
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
            project('api', ':annotation-1'),
          )
        }
      }
    // Used?
      .withSubproject('annotation-2') { s ->
        s.sources = sourcesAnnotation2
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

        import com.example.annotation.Annotate;

        @Annotate(0)
        public class Consumer {
        }
      '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  private static final List<Source> sourcesAnnotation1 = [
    Source.java(
      '''\
        package com.example.annotation;

        import java.lang.annotation.Target;
        import java.lang.annotation.Retention;

        import static java.lang.annotation.RetentionPolicy.RUNTIME;
        import static java.lang.annotation.ElementType.TYPE;

        @Target(TYPE)
        @Retention(RUNTIME)
        public @interface Annotate {
          String value() default "";
        }
      '''
    )
      .withPath('com.example.annotation', 'Annotate')
      .build(),
  ]

  // Same class file as sourcesAnnotation1, incompatible definition
  private static final List<Source> sourcesAnnotation2 = [
    Source.java(
      '''\
        package com.example.annotation;

        import java.lang.annotation.Target;
        import java.lang.annotation.Retention;

        import static java.lang.annotation.RetentionPolicy.RUNTIME;
        import static java.lang.annotation.ElementType.TYPE;

        @Target(TYPE)
        @Retention(RUNTIME)
        public @interface Annotate {
          int value() default 0;
        }
      '''
    )
      .withPath('com.example.annotation', 'Annotate')
      .build(),
  ]

  private static final List<Source> sourcesUnused = [
    Source.java(
      '''\
        package com.example.unused;

        import com.example.annotation.Annotate;

        @Annotate("")
        public class Unused {
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
    Advice.ofAdd(projectCoordinates(':annotation-1'), 'api')
  ]

  Set<ProjectAdvice> expectedProjectAdvice() {
    return [
      projectAdviceForDependencies(':consumer', consumerAdvice, shouldFail),
      emptyProjectAdviceFor(':unused'),
      emptyProjectAdviceFor(':annotation-1'),
      emptyProjectAdviceFor(':annotation-2'),
    ]
  }

  static class DagpConfiguration {

    private final String onAnySeverity
    private final List<String> duplicateClassesFilter
    private final String duplicateClassesSeverity

    DagpConfiguration(String onAnySeverity, List<String> duplicateClassesFilter, String duplicateClassesSeverity) {
      this.onAnySeverity = onAnySeverity
      this.duplicateClassesFilter = duplicateClassesFilter
      this.duplicateClassesSeverity = duplicateClassesSeverity
    }

    @Override
    String toString() {
      def builder = new StringBuilder()
      builder.append('dependencyAnalysis {\n')
      builder.append('  reporting {\n')
      builder.append('    onlyOnFailure(true)\n')
      // multiline for a "manual test" of colorized support for multiline strings
      builder.append('    postscript("""MULTILINE\nERRORS-ONLY POSTSCRIPT""")\n')
      builder.append('  }\n')

      builder.append('  issues {\n')
      builder.append('    all {\n')
      builder.append('      onAny {\n')
      builder.append("        severity \'$onAnySeverity\'\n")
      builder.append('      }\n')

      if (duplicateClassesFilter || duplicateClassesSeverity) {
        builder.append('      onDuplicateClassWarnings {\n')
        if (duplicateClassesSeverity) {
          builder.append("        severity \'$duplicateClassesSeverity\'\n")
        }
        duplicateClassesFilter.each {
          builder.append("        exclude \'$it\'\n")
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
