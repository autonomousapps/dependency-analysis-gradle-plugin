// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.testFixturesImplementation

final class ReasonProject extends AbstractProject {

  final GradleProject gradleProject

  ReasonProject() {
    gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { p ->
        p.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary, Plugin.javaTestFixtures, Plugins.dependencyAnalysisNoVersion]
          bs.dependencies(
            implementation(':producer'),
            implementation(':producer').onTestFixtures(),
            testFixturesImplementation(':producer'),
          )
        }
        p.sources = sourcesConsumer
      }
      .withSubproject('producer') { p ->
        p.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary, Plugin.javaTestFixtures, Plugins.dependencyAnalysisNoVersion]
        }
        p.sources = sourcesProducer
      }
      .write()
  }

  protected List<Source> sourcesConsumer = [
    Source.java(
      '''\
          package com.example.consumer;
          
          import com.example.producer.Person;
          import com.example.producer.Simpsons;

          public class Consumer {
            // Uses test fixtures of producer and main artifact as well
            private Person homer = Simpsons.HOMER;
          }
        '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
    Source.java(
      '''\
          package com.example.consumer;
          
          import com.example.producer.Person;

          class TestFixture {
            // uses main artifact of producer
            private Person testPerson = new Person("Emma", "Goldman");
          }
        '''
    )
      .withPath('com.example.consumer', 'TestFixture')
      .withSourceSet('testFixtures')
      .build(),
  ]

  protected List<Source> sourcesProducer = [
    Source.java(
      '''\
          package com.example.producer;

          public class Person {
            public final String firstName;
            public final String lastName;
            
            public Person(String firstName, String lastName) {
              this.firstName = firstName;
              this.lastName = lastName;
            }
          }
        '''
    )
      .withPath('com.example.producer', 'Person')
      .build(),
    Source.java(
      '''\
          package com.example.producer;

          public final class Simpsons {
            public static Person HOMER = new Person("Homer", "Simpson");
          }
        '''
    )
      .withPath('com.example.producer', 'Simpsons')
      .withSourceSet('testFixtures')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer'),
  ]
}
