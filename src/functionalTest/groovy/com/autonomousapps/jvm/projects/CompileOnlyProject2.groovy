package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class CompileOnlyProject2 extends AbstractProject {

  final GradleProject gradleProject

  CompileOnlyProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('implementation', ':producer'),
          ]
        }
      }
      .withSubproject('producer') { s ->
        s.sources = SOURCE_PRODUCER
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final Source SOURCE_CONSUMER = Source.java(
    '''\
      package com.example.consumer;
      
      import com.example.producer.Producer;
      
      @Producer
      public class Consumer {}
    '''
  )
    .withPath('com.example.consumer', 'Consumer')
    .build()

  private static final List<Source> SOURCE_PRODUCER = [
    Source.java(
      '''\
        package com.example.producer;

        import java.lang.annotation.*;
      
        @Target({ElementType.TYPE})
        @Retention(RetentionPolicy.CLASS)
        public @interface Producer {}
      '''
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'compileOnly')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
  ]
}
