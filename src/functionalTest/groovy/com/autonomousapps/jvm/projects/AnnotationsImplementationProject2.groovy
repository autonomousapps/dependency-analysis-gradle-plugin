package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.compileOnly

final class AnnotationsImplementationProject2 extends AbstractProject {

  final GradleProject gradleProject

  AnnotationsImplementationProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = SOURCE_CONSUMER
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            compileOnly('org.cthing:cthing-annotations:1.0.0'),
          )

          // "cthing" uses Java 17+
          bs.java = Java.of(17)
        }
      }
      .write()
  }

  private static final List<Source> SOURCE_CONSUMER = [
    Source.java(
      '''\
        @PackageNonnullByDefault
        package com.example.consumer;
        
        import org.cthing.annotations.PackageNonnullByDefault;
      '''
    )
      .withPath('com.example.consumer', 'package-info')
      .build(),
    Source.java(
      '''\
      package com.example.consumer;
      
      public class Consumer {}
    '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
