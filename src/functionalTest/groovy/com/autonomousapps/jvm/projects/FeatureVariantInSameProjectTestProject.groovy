// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Feature
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class FeatureVariantInSameProjectTestProject extends AbstractProject {

  final GradleProject gradleProject

  FeatureVariantInSameProjectTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('single') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.java = Java.ofFeatures(Feature.ofName('extraFeature'))
          bs.dependencies = [
            project('extraFeatureApi', ':single')
          ]
        }
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        public class Example {
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExtraFeature", "com/example/extra",
      """\
        package com.example.extra;
        
        import com.example.Example;
        
        public class ExtraFeature {
          private Example e;
        }
      """.stripIndent(),
      "extraFeature"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedAdvice = [
    Advice.ofChange(projectCoordinates(':single'), 'extraFeatureApi', 'extraFeatureImplementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':single', expectedAdvice),
  ]
}
