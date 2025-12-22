// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BinaryIncompatibilityProject
import com.autonomousapps.jvm.projects.DuplicateAnnotationClasspathProject
import com.autonomousapps.jvm.projects.DuplicateClasspathProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class DuplicateClasspathSpec extends AbstractJvmSpec {

  // TODO(tsr): missing test for when there's duplication and all accesses are in fact compatible.

  def "duplicates on the classpath can be fixed (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject()
    gradleProject = project.gradleProject

    when:
    // This first invocation fixes the dependency declarations
    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')
    // This succeeds because we detect the binary incompatibility and remove it
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(gradleProject.rootDir.toPath().resolve('consumer/build.gradle').text).contains(
      '''\
        dependencies {
          implementation project(':producer-2')
        }'''.stripIndent()
    )

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "buildHealth reports duplicates (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).contains(
      '''\
        Warnings
        Some of your classpaths have duplicate classes, which means the compile and runtime behavior can be sensitive to the classpath order.
        
        Source set: main
        \\--- compile classpath
             +--- com/example/producer/Producer is provided by multiple dependencies: [:producer-1, :producer-2]
             \\--- com/example/producer/Producer$Inner is provided by multiple dependencies: [:producer-1, :producer-2]
        \\--- runtime classpath
             +--- com/example/producer/Producer is provided by multiple dependencies: [:producer-1, :producer-2]
             \\--- com/example/producer/Producer$Inner is provided by multiple dependencies: [:producer-1, :producer-2]'''
        .stripIndent()
    )

    and: 'Postscript is printed'
    assertThat(result.output).contains('ERRORS-ONLY POSTSCRIPT')

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "buildHealth reports duplicates in annotations (#gradleVersion)"() {
    given:
    def project = new DuplicateAnnotationClasspathProject()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).contains(
      '''\
        Warnings
        Some of your classpaths have duplicate classes, which means the compile and runtime behavior can be sensitive to the classpath order.
        
        Source set: main
        \\--- compile classpath
             \\--- com/example/annotation/Annotate is provided by multiple dependencies: [:annotation-1, :annotation-2]
        \\--- runtime classpath
             \\--- com/example/annotation/Annotate is provided by multiple dependencies: [:annotation-1, :annotation-2]'''
        .stripIndent()
    )

    and: 'Postscript is printed'
    assertThat(result.output).contains('ERRORS-ONLY POSTSCRIPT')

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "buildHealth reports filters duplicates (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject('fail', 'com/example/producer/Producer$Inner')
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).contains(
      '''\
        Warnings
        Some of your classpaths have duplicate classes, which means the compile and runtime behavior can be sensitive to the classpath order.
        
        Source set: main
        \\--- compile classpath
             \\--- com/example/producer/Producer is provided by multiple dependencies: [:producer-1, :producer-2]
        \\--- runtime classpath
             \\--- com/example/producer/Producer is provided by multiple dependencies: [:producer-1, :producer-2]'''
        .stripIndent()
    )

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "buildHealth reports ignores duplicates (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject('fail', null, 'ignore')
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).doesNotContain('Warnings')

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "postscript can be configured to not print for only warnings (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject('warn')
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).doesNotContain('ERRORS-ONLY POSTSCRIPT')

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "can report on which of the duplicates is needed for binary compatibility (#gradleVersion)"() {
    given:
    def project = new BinaryIncompatibilityProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      ':consumer:reason', '--id', ':producer-1',
    )

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        ------------------------------------------------------------
        You asked about the dependency ':producer-1'.
        There is no advice regarding this dependency.
        ------------------------------------------------------------
        
        Shortest path from :consumer to :producer-1 for compileClasspath:
        :consumer
        \\--- :unused
              \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for runtimeClasspath:
        :consumer
        \\--- :unused
              \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for testCompileClasspath:
        :consumer
        \\--- :unused
              \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for testRuntimeClasspath:
        :consumer
        \\--- :unused
              \\--- :producer-1
        
        Source: main
        ------------
        * Is binary-incompatible, and should be removed from the classpath:
          Expected METHOD com/example/producer/Person.<init>(Ljava/lang/String;Ljava/lang/String;)V, but was com/example/producer/Person.<init>(Ljava/lang/String;)V
        
        Source: test
        ------------
        (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "suggests removing a binary-incompatible duplicate (#gradleVersion)"() {
    given:
    def project = new BinaryIncompatibilityProject(true)
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      ':consumer:reason', '--id', ':producer-1',
    )

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        ------------------------------------------------------------
        You asked about the dependency ':producer-1'.
        You have been advised to remove this dependency from 'implementation'.
        ------------------------------------------------------------
        
        Shortest path from :consumer to :producer-1 for compileClasspath:
        :consumer
        \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for runtimeClasspath:
        :consumer
        \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for testCompileClasspath:
        :consumer
        \\--- :producer-1
        
        Shortest path from :consumer to :producer-1 for testRuntimeClasspath:
        :consumer
        \\--- :producer-1
        
        Source: main
        ------------
        * Is binary-incompatible, and should be removed from the classpath:
          Expected METHOD com/example/producer/Person.<init>(Ljava/lang/String;Ljava/lang/String;)V, but was com/example/producer/Person.<init>(Ljava/lang/String;)V
          
        Source: test
        ------------
        (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << [GRADLE_LATEST]
  }
}
