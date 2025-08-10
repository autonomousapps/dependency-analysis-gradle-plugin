// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class CircularDependencyProject extends AbstractProject {

  final GradleProject gradleProject

  CircularDependencyProject() {
    this.gradleProject = build()
  }

  /**
   * Circular dependency:
   *
   * <pre>
   * ':core' --> testImplementation ':utils-test' --> implementation ':core'
   * </pre>
   *
   * This is valid in Gradle. However, at the time of writing, this setup leads to the following advice:
   *
   * <pre>
   * Advice for :core
   * These transitive dependencies should be declared directly:
   *   testImplementation project(':core')
   * </pre>
   *
   * And we don't want to ask the user to add a {@code testImplementation} dependency onto the project itself.
   */
  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('core') { s ->
        s.sources = coreSources
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(project('testImplementation', ':utils-test'))
        }
      }
      .withSubproject('utils-test') { s ->
        s.sources = utilsTestSources
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(project('api', ':core'))
        }
      }
      .write()
  }

  private static final List<Source> coreSources = [
    Source.java(
      """
      package core;

      public abstract class AbstractBaseClass {}
      """
    )
      .withPath('core', 'AbstractBaseClass')
      .build(),
    Source.java(
      """
      package core;

      public class CoolUtility {}
      """
    )
      .withPath('core', 'CoolUtility')
      .build(),
    Source.java(
      """
      package core.test;

      import core.CoolUtility;
      import utils.test.RealClass;

      public abstract class Test {
        public void test() {
          CoolUtility coolUtility = new CoolUtility();
          RealClass realClass = new RealClass();
        }
      }
      """
    )
      .withPath('core.test', 'Test')
      .withSourceSet('test')
      .build()
  ]

  private static final List<Source> utilsTestSources = [
    Source.java(
      """
      package utils.test;

      import core.AbstractBaseClass;

      public final class RealClass extends AbstractBaseClass {}
      """
    )
      .withPath('utils.test', 'RealClass')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':core'),
    emptyProjectAdviceFor(':utils-test')
  ]
}
