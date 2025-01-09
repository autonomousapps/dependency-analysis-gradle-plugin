package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class MissingSuperclassProject extends AbstractProject {

  final GradleProject gradleProject

  MissingSuperclassProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
    // TODO: update comments
    // :consumer uses the Producer class.
    // This class is provided by both
    // 1. :producer-2, which is a direct dependency, and
    // 2. :producer-1, which is a transitive dependency (of :unused)
    // These classes have incompatible definitions. :consumer _requires_ the version provided by :producer-2.
      .withSubproject('a') { s ->
        s.sources = sourcesA
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + plugins.gradleDependenciesSorter
          bs.dependencies(
            project('api', ':b'),
            // Shouldn't need this. Use it because ":b" has broken metadata
            project('implementation', ':c'),
          )

          // TODO: remove this if unused
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
      .withSubproject('b') { s ->
        s.sources = sourcesB
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            // Should be "api"
            project('implementation', ':c'),
          )
        }
      }
      .withSubproject('c') { s ->
        s.sources = sourcesC
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final List<Source> sourcesA = [
    Source.java(
      '''\
        package com.example.a;

        import com.example.b.B;

        public class A extends B {}
      '''
    )
      .withPath('com.example.a', 'A')
      .build(),
  ]

  private static final List<Source> sourcesB = [
    Source.java(
      '''\
        package com.example.b;

        import com.example.c.C;

        public class B extends C {}
      '''
    )
      .withPath('com.example.b', 'B')
      .build(),
  ]

  private static final List<Source> sourcesC = [
    Source.java(
      '''\
        package com.example.c;

        public class C {}
      '''
    )
      .withPath('com.example.c', 'C')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> adviceA = [
    Advice.ofRemove(projectCoordinates(':c'), 'implementation'),
  ]

  private final Set<Advice> adviceB = [
    Advice.ofChange(projectCoordinates(':c'), 'implementation', 'api'),
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':a', adviceA),
    projectAdviceForDependencies(':b', adviceB),
    emptyProjectAdviceFor(':c'),
  ]
}
