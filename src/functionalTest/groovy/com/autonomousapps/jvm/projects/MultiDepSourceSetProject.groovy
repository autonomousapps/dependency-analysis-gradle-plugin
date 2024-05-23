package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Feature
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/947">Issue 947</a>
 */
final class MultiDepSourceSetProject extends AbstractProject {

  private final Dependency okio380 = implementation('com.squareup.okio:okio:3.8.0')
  private final Dependency okio390 = new Dependency('extraApi', 'com.squareup.okio:okio:3.9.0')

  final GradleProject gradleProject

  MultiDepSourceSetProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.java = Java.ofFeatures(Feature.ofName('extra'))
          bs.dependencies = [okio380, okio390]
        }
      }
      .write()
  }

  private consumerSources = [
    Source
      .java(
        '''
          package com.example;
          
          import okio.Buffer;
          
          public class Project {
            // implementation but should be api (bc public)
            public Buffer buffer;
          }
        '''.stripIndent()
      )
      .withPath('com.example', 'Project')
      .build(),
    Source
      .java(
        '''
          package com.example.extra;
          
          import okio.Buffer;
          
          public class Extra {
            // extraApi but should be extraImplementation (bc private)
            private Buffer buffer;
          }
        '''.stripIndent()
      )
      .withPath('com.example.extra', 'Extra')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedAdvice = [
    Advice.ofChange(moduleCoordinates(okio380), 'implementation', 'api'),
    Advice.ofChange(moduleCoordinates(okio390), 'extraApi', 'extraImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', expectedAdvice),
  ]
}
