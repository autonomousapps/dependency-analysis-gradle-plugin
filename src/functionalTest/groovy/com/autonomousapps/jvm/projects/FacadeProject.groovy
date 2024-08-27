package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class FacadeProject extends AbstractProject {

  final GradleProject gradleProject

  FacadeProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
    // consumer -> unused -> used
    // consumer -> used
    // :unused is just a facade that points to :used
    // consumer uses :used.
      .withSubproject('consumer') { c ->
        c.sources = sourcesConsumer
        c.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('implementation', ':unused'),
            project('implementation', ':used'),
          ]
        }
      }
      .withSubproject('unused') { s ->
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('api', ':used')
          ]
        }
      }
      .withSubproject('used') { s ->
        s.sources = sourcesUsed
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.JAVA, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer;
        
        import com.example.used.Used;
        
        public class Consumer {
          private Used used;
        }
      """.stripIndent()
    )
  ]

  private sourcesUsed = [
    new Source(
      SourceType.JAVA, 'Used', 'com/example/used',
      """\
        package com.example.used;
        
        public class Used {}
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation')
  ]

  private final Set<Advice> unusedAdvice = [
    Advice.ofRemove(projectCoordinates(':used'), 'api')
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':unused', unusedAdvice),
    emptyProjectAdviceFor(':used')
  ]
}
