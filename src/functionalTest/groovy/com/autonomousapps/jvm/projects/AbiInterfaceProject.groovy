package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.project

class AbiInterfaceProject extends AbstractProject {

  final GradleProject gradleProject
  private final abstractProject = project('implementation', ':abstract')

  AbiInterfaceProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    // consumer
    builder.withSubproject('impl') { s ->
      s.sources = [SOURCE_CONSUMER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [abstractProject]
      }
    }
    builder.withSubproject('abstract') { s ->
      s.sources = [SOURCE_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = []
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE_PRODUCER = new Source(
    SourceType.JAVA, "IPermission", "",
    """\
      public interface IPermission {
        String name();
        boolean isAdmin();
        default Object getLoggingObject() {
          return name();
        }
      }
     """.stripIndent()
  )

  private static final Source SOURCE_CONSUMER = new Source(
    SourceType.JAVA, "WebPermission", "",
    """\
      public enum WebPermission implements IPermission {
        ViewData(false),
        EditData(true),
        ;
        
        private final boolean superRole;
        
        WebPermission(boolean superRole) {
          this.superRole = superRole;
        }
        
        @Override
        public boolean isAdmin() {
          return superRole;
        }
      }
     """.stripIndent()
  )

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  private final implAdvice = [
    Advice.ofChange(dependency(abstractProject), 'api')
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    compAdviceForDependencies(':impl', implAdvice),
    emptyCompAdviceFor(':abstract'),
  ]
}
