// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.compileOnly
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class ExceptionsAreSpecialProject extends AbstractProject {

  // Should be implementation, but isn't. Consumer advice
  private static final Dependency JSON_COMPILE_ONLY = compileOnly('org.json:json:20251224')
  private static final Dependency JSON_IMPL = implementation('org.json:json:20251224')

  private final boolean isBroken
  final GradleProject gradleProject

  ExceptionsAreSpecialProject(boolean isBroken) {
    this.isBroken = isBroken
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          // should be runtimeOnly to account for the bad metadata in ':producer'
          bs.dependencies(
            implementation(':producer'),
            JSON_IMPL,
          )
        }
      }
    // Here we should assume that ':producer' is actually an external artifact we have no control over.
      .withSubproject('producer') { s ->
        s.sources = producerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(isBroken ? JSON_COMPILE_ONLY : JSON_IMPL)
        }
      }
    // TODO(tsr): another module that has `throws JSONException` to check if that should be `api`?
      .write()
  }

  private static final List<Source> consumerSources() {
    return [
      Source.java(
        '''\
          package mutual.aid.consumer;
          
          import mutual.aid.producer.Producer;
          
          public class Consumer {
            public void ohno() {
              Producer producer = new Producer();
            }
          }
        '''.stripIndent()
      ).build()
    ]
  }

  private static final List<Source> producerSources() {
    return [
      Source.java(
        '''\
          package mutual.aid.producer;
          
          import org.json.JSONException;
          
          public class Producer {
            public void ohno() {
              try {
                System.out.println("try");
              } catch(JSONException e) {
                System.out.println("catch");
              }
            }
          }
        '''.stripIndent()
      ).build()
    ]
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice() {
    if (isBroken) {
      [Advice.ofChange(moduleCoordinates(JSON_IMPL), JSON_IMPL.configuration, 'runtimeOnly')]
    } else {
      [Advice.ofRemove(moduleCoordinates(JSON_IMPL), JSON_IMPL.configuration)]
    }

  }

  // Current behavior of `compileOnly` is we don't advise to change it. Obviously it can be wrong but not sure what we
  // can realistically do here.
  private static final Set<Advice> producerAdvice() {
    return [
//      Advice.ofChange(moduleCoordinates(JSON_COMPILE_ONLY), JSON_COMPILE_ONLY.configuration, 'implementation')
    ]
  }

  Set<ProjectAdvice> expectedProjectAdvice() {
    return [
      projectAdviceForDependencies(':consumer', consumerAdvice()),
      projectAdviceForDependencies(':producer', producerAdvice()),
    ]
  }

  String expectedReason() {
    if (isBroken) {
      '''\
      Source: main
      ------------
      * Referenced 1 time by another dependency: (1) ':producer': (a) [org.json.JSONException] by class mutual.aid.producer.Producer (implies runtimeOnly).'''.stripIndent()
    } else {
      '''\
      Source: main
      ------------
      (no usages)'''.stripIndent()
    }
  }
}
