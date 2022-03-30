package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.*

/**
 * <pre>
 * import com.my.other.DependencyClass;
 * import java.util.Optional;
 *
 * public interface MyJavaClass {*   Optional<DependencyClass> getMyDependency();
 *}* </pre>
 *
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/148
 */
final class GenericsProject extends AbstractProject {

  final GradleProject gradleProject

  GenericsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj-1') { s ->
      s.sources = sources1
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = [new Dependency('implementation', ':proj-2')]
      }
    }
    builder.withSubproject('proj-2') { s ->
      s.sources = sources2
      s.withBuildScript { bs ->
        bs.plugins = plugins
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final List<Plugin> plugins = [Plugin.javaLibraryPlugin]

  private final List<Source> sources1 = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        import com.example.lib.Library;
        import java.util.Optional;

        public interface Main {
          Optional<Library> getLibrary();
        }
      """.stripIndent()
    )
  ]
  private final List<Source> sources2 = [
    new Source(
      SourceType.JAVA, 'Library', 'com/example/lib',
      """\
        package com.example.lib;
        
        public class Library {
        }
      """.stripIndent()
    )
  ]

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  private final proj1Advice = [
    Advice.ofChange(dependency(
      identifier: ':proj-2', configurationName: 'implementation'
    ), 'api')
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    compAdviceForDependencies(':proj-1', proj1Advice),
    emptyCompAdviceFor(':proj-2'),
  ]
}
