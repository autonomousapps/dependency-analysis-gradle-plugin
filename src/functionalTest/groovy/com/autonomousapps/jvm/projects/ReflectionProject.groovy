// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class ReflectionProject extends AbstractProject {

  final GradleProject gradleProject

  ReflectionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(implementation(':direct'))
        }
        s.sources = consumerSources
      }
      .withSubproject('direct') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(api(':uses-reflection'))
        }
        s.sources = directSources
      }
      .withSubproject('uses-reflection') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.sources = usesReflectionSources
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.java(
      '''\
        package com.example.consumer;
        
        import com.example.reflection.Thing;
        
        public class Consumer {
          public void usesThing() {
            new Thing();
          }
        }'''.stripIndent()
    ).build(),
  ]

  private List<Source> directSources = [
    Source.java(
      '''\
        package com.example.direct;
        
        import com.example.reflection.Thing;
        
        public class Direct {
        
          public class Inner {}
          public static class StaticInner {}
        
          public Thing createsThing() {
            return new Thing();
          }
        }'''.stripIndent()
    ).build(),
  ]

  private List<Source> usesReflectionSources = [
    Source.java(
      '''\
        package com.example.reflection;
                
        public class UsesReflection {
          public void usesReflection() {
            try {
              Class.forName("com.example.direct.Direct");
              Class.forName("com.example.direct.Direct$Inner");
              Class.forName("com.example.direct.Direct$StaticInner");
            } catch (Exception e) {
            }
          }
        }'''.stripIndent()
    ).build(),
    Source.java(
      '''\
        package com.example.reflection;
                
        public class Thing {}'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':direct'), 'implementation', 'runtimeOnly'),
    Advice.ofAdd(projectCoordinates(':uses-reflection'), 'implementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':direct'),
    emptyProjectAdviceFor(':uses-reflection'),
  ]
}
