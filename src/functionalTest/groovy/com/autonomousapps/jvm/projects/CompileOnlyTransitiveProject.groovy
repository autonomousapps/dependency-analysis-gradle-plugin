package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.compileOnly

final class CompileOnlyTransitiveProject extends AbstractProject {

  final GradleProject gradleProject

  CompileOnlyTransitiveProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { p ->
        p.sources = consumerSources
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(compileOnly(':direct'))
        }
      }
      .withSubproject('direct') { p ->
        p.sources = directSources
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(api(':transitive'))
        }
      }
      .withSubproject('transitive') { p ->
        p.sources = transitiveSources
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.java(
      '''\
      package com.example.consumer;
      
      import com.example.transitive.Transitive;
      
      public class Consumer {
        // part of the ABI, but we declare ":transitive"'s parent on `compileOnly` 
        public void accept(Transitive transitive) {}
      }
      '''.stripIndent()
    )
      .build(),
  ]

  private List<Source> directSources = [
    Source.java(
      '''\
      package com.example.direct;
      
      import com.example.transitive.Transitive;
      
      public class Direct {
        public void accept(Transitive transitive) {}
      }
      '''.stripIndent()
    )
      .build(),
  ]

  private List<Source> transitiveSources = [
    Source.java(
      '''\
      package com.example.transitive;
            
      public class Transitive {}
      '''.stripIndent()
    )
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':direct'),
    emptyProjectAdviceFor(':transitive'),
  ]
}
