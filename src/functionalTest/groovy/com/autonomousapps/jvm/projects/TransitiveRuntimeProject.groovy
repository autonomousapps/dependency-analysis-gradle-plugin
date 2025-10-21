// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.testImplementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.conscryptUber

final class TransitiveRuntimeProject extends AbstractProject {

  private final conscryptUber = conscryptUber('api')
  private final serviceLoader = api(':service-loader')
  final GradleProject gradleProject

  TransitiveRuntimeProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(
            implementation(':unused'),
            testImplementation(':unused-for-test')
          )
        }
      }
      .withSubproject('unused') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(conscryptUber)
        }
      }
      .withSubproject('unused-for-test') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(serviceLoader)
        }
      }
      .withSubproject('service-loader') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
        s.withFile('src/main/resources/META-INF/services/com.example.service.Service', 'com.example.service.RealService')
        s.sources = serviceLoaderSources
      }
      .write()
  }

  private List<Source> serviceLoaderSources = [
    Source.java(
      '''\
        package com.example.service;
        
        public interface Service {}'''.stripIndent()
    ).build(),
    Source.java(
      '''\
        package com.example.service;
        
        public class RealService implements Service {}'''.stripIndent()
    ).build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation'),
    Advice.ofRemove(projectCoordinates(':unused-for-test'), 'testImplementation'),
    Advice.ofAdd(moduleCoordinates(conscryptUber), 'runtimeOnly'),
    Advice.ofAdd(projectCoordinates(serviceLoader), 'testRuntimeOnly'),
  ]

  private final Set<Advice> unusedAdvice = [
    Advice.ofChange(moduleCoordinates(conscryptUber), conscryptUber.configuration, 'runtimeOnly'),
  ]

  private final Set<Advice> unusedForTestAdvice = [
    Advice.ofChange(projectCoordinates(':service-loader'), 'api', 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':unused', unusedAdvice),
    projectAdviceForDependencies(':unused-for-test', unusedForTestAdvice),
    emptyProjectAdviceFor(':service-loader'),
  ]
}
