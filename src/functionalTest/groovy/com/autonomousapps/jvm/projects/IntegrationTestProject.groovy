package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class IntegrationTestProject extends AbstractProject {

  final GradleProject gradleProject

  final boolean withCorrectIntegrationTestDependencies

  IntegrationTestProject(boolean withCorrectIntegrationTestDependencies) {
    this.withCorrectIntegrationTestDependencies = withCorrectIntegrationTestDependencies
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = PROJ_SOURCES
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.sourceSets = ['integrationTest']
        bs.dependencies = [
          project('implementation', ':lib'),
          project('integrationTestImplementation', withCorrectIntegrationTestDependencies ? ':core' : ':lib')
        ]
      }
    }
    builder.withSubproject('lib') { s ->
      s.sources = [LIB_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          project('api', ':core')
        ]
      }
    }
    builder.withSubproject('core') { s ->
      s.sources = [CORE_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> PROJ_SOURCES = [
    new Source(SourceType.JAVA, "MainApplication", "com/example",
      """\
        package com.example;
        
        import com.example.core.NotificationSender;
        import com.example.lib.EmailSender;

        public class MainApplication {
          private final NotificationSender sender;
          
          public MainApplication() {
            this(new EmailSender());
          }

          public MainApplication(NotificationSender sender) {
            this.sender = sender;
          }

          public void sendMessage(String message) {
            sender.send(message);
          }
        }""".stripIndent()
    ),
    new Source(SourceType.JAVA, "MockEmailChannel", "com/example/integrationTest",
      """\
        package com.example.integrationTest;
        
        import com.example.core.NotificationSender;

        public class MockEmailChannel extends NotificationSender {
          @Override
          public void send(String message) {
            // Don't send email on CI
          }
        }""".stripIndent(),
      'integrationTest'
    )
  ]

  private Source LIB_PRODUCER = new Source(SourceType.JAVA, "EmailSender", "com/example/lib",
    """\
      package com.example.lib;
      
      import com.example.core.NotificationSender;
      
      public class EmailSender extends NotificationSender {
        @Override
        public void send(String message) {
          // Some true implementation
        }
      }""".stripIndent()
  )

  private Source CORE_PRODUCER = new Source(SourceType.JAVA, "NotificationSender", "com/example/core",
    """\
      package com.example.core;
      
      public abstract class NotificationSender {
        public abstract void send(String message);
      }""".stripIndent()
  )

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    if (withCorrectIntegrationTestDependencies) {
      [
        emptyProjectAdviceFor(':lib'),
        emptyProjectAdviceFor(':core'),
        projectAdviceForDependencies(':proj', [
          Advice.ofAdd(projectCoordinates(':core'), 'api')
        ] as Set<Advice>)
      ]
    } else {
      [
        emptyProjectAdviceFor(':lib'),
        emptyProjectAdviceFor(':core'),
        projectAdviceForDependencies(':proj', [
          Advice.ofAdd(projectCoordinates(':core'), 'api'),
          Advice.ofAdd(projectCoordinates(':core'), 'integrationTestImplementation'),
          Advice.ofRemove(projectCoordinates(':lib'), 'integrationTestImplementation')
        ] as Set<Advice>)
      ]
    }
  }
}
