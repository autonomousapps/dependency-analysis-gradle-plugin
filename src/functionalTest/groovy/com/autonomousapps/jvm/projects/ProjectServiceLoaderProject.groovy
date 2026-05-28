// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class ProjectServiceLoaderProject extends AbstractProject {

  private final serviceLoader = implementation(':service-loader')
  final GradleProject gradleProject

  ProjectServiceLoaderProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
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
    Advice.ofChange(projectCoordinates(':service-loader'), serviceLoader.configuration, 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':service-loader'),
  ]
}
