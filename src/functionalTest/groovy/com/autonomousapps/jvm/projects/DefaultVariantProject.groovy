package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.commonsIO

final class DefaultVariantProject {

  static final class Java extends AbstractProject {

    final GradleProject gradleProject
    private final projectName = 'lib'

    Java() {
      gradleProject = build()
    }

    private GradleProject build() {
      def builder = newGradleProjectBuilder()
      builder.withSubproject(projectName) { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibraryPlugin]
          bs.dependencies = [commonsIO('implementation')]
        }
      }

      def project = builder.build()
      project.writer().write()
      return project
    }

    private List<Source> sources = [
      new Source(
        SourceType.JAVA, 'Main', 'com/example',
        """\
        package com.example;
        
        import org.apache.commons.io.output.ByteArrayOutputStream;
        
        public class Main {
          static class StaticNestedClass {
            private static ByteArrayOutputStream method() {
              return new ByteArrayOutputStream();
            }
          }
        }
      """.stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'Test', 'com/example',
        """\
        package com.example;
        
        import org.apache.commons.io.output.ByteArrayOutputStream;
        
        public class Test {
          private static ByteArrayOutputStream method() {
            return new ByteArrayOutputStream();
          }
        }
      """.stripIndent(),
        'test'
      )
    ]

    @SuppressWarnings('GroovyAssignabilityCheck')
    List<ComprehensiveAdvice> actualBuildHealth() {
      actualBuildHealth(gradleProject)
    }

    final List<ComprehensiveAdvice> expectedBuildHealth = [
      emptyCompAdviceFor(':lib'),
    ]
  }

  static final class Kotlin extends AbstractProject {

    final GradleProject gradleProject
    private final projectName = 'lib'

    Kotlin() {
      gradleProject = build()
    }

    private GradleProject build() {
      def builder = newGradleProjectBuilder()
      builder.withSubproject(projectName) { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.kotlinPluginNoVersion]
          bs.dependencies = [commonsIO('implementation')]
        }
      }

      def project = builder.build()
      project.writer().write()
      return project
    }

    private List<Source> sources = [
      new Source(
        SourceType.KOTLIN, 'Main', 'com/example',
        """\
        package com.example
        
        import org.apache.commons.io.output.ByteArrayOutputStream
        
        class Foo {
          private fun method(): ByteArrayOutputStream = ByteArrayOutputStream()
          private fun otherMethod() = topLevelMethod()
        }
        
        private fun topLevelMethod() = ByteArrayOutputStream()
      """.stripIndent()
      ),
      new Source(
        SourceType.KOTLIN, 'Test', 'com/example',
        """\
        package com.example
        
        import org.apache.commons.io.output.ByteArrayOutputStream
        
        class Test {
          private fun method(): ByteArrayOutputStream = ByteArrayOutputStream()
        }
      """.stripIndent(),
        'test'
      )
    ]

    @SuppressWarnings('GroovyAssignabilityCheck')
    List<ComprehensiveAdvice> actualBuildHealth() {
      actualBuildHealth(gradleProject)
    }

    final List<ComprehensiveAdvice> expectedBuildHealth = [
      emptyCompAdviceFor(':lib'),
    ]
  }
}
