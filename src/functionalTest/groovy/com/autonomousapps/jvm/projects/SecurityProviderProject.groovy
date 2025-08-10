// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.conscryptUber
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okHttp

final class SecurityProviderProject extends AbstractProject {

  final GradleProject gradleProject

  SecurityProviderProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = dependencies
        }
      }
      .write()
  }

  private final conscryptUber = conscryptUber("implementation")

  private List<Dependency> dependencies = [
    conscryptUber,
    okHttp("api")
  ]

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import okhttp3.OkHttpClient;

        public class Main {
          public OkHttpClient ok() {
            return new OkHttpClient.Builder().build();
          }
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final projAdvice = [
    Advice.ofChange(moduleCoordinates(conscryptUber), conscryptUber.configuration, 'runtimeOnly'),
  ] as Set<Advice>

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', projAdvice)
  ]
}
