// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BundleKmpProject2
import com.autonomousapps.jvm.projects.NestedSubprojectsProject
import com.autonomousapps.jvm.projects.ReasonProject
import com.autonomousapps.utils.Colors
import org.gradle.testkit.runner.BuildResult
import spock.lang.PendingFeature

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class ReasonSpec extends AbstractJvmSpec {

  def "can discover reason for project dependency defined by project path (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = ':featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        ------------------------------------------------------------
        You asked about the dependency ':featureA:public'.
        You have been advised to remove this dependency from 'api'.
        ------------------------------------------------------------
        
        Shortest path from :featureC:public to :featureA:public for compileClasspath:
        :featureC:public
        \\--- :featureA:public
        
        Shortest path from :featureC:public to :featureA:public for runtimeClasspath:
        :featureC:public
        \\--- :featureA:public
        
        Shortest path from :featureC:public to :featureA:public for testCompileClasspath:
        :featureC:public
        \\--- :featureA:public
        
        Shortest path from :featureC:public to :featureA:public for testRuntimeClasspath:
        :featureC:public
        \\--- :featureA:public
        
        Source: main
        ------------
        (no usages)
        
        Source: test
        ------------
        (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  @PendingFeature(reason = "ReasonTask doesn't support this because ProjectAdvice.dependencyAdvice uses ProjectCoordinates, not IncludedBuildCoordinates")
  def "can discover reason for project dependency defined by coordinates (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = 'the-project.featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    outputMatchesForProject(result, id)

    where:
    gradleVersion << gradleVersions()
  }

  def "reason fails when there is dependency filtering ambiguity (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject2()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:oki')

    then:
    assertThat(result.output).contains(
      "> Coordinates 'com.squareup.okio:oki' matches more than 1 dependency [com.squareup.okio:okio-jvm:3.0.0, com.squareup.okio:okio:3.0.0]")

    where:
    gradleVersion << gradleVersions()
  }

  def "reason matches startsWith when there is no ambiguity (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject2()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:okio-')

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
            ------------------------------------------------------------
            You asked about the dependency 'com.squareup.okio:okio-jvm:3.0.0'.
            There is no advice regarding this dependency.
            It was removed because it matched a bundle rule for com.squareup.okio:okio:3.0.0, which is already present in the dependency graph.
            ------------------------------------------------------------
            
            Shortest path from :consumer to com.squareup.okio:okio-jvm:3.0.0 for compileClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
                        \\--- com.squareup.okio:okio-jvm:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio-jvm:3.0.0 for runtimeClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
                        \\--- com.squareup.okio:okio-jvm:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio-jvm:3.0.0 for testCompileClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
                        \\--- com.squareup.okio:okio-jvm:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio-jvm:3.0.0 for testRuntimeClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
                        \\--- com.squareup.okio:okio-jvm:3.0.0
            
            Source: main
            ------------
            * Exposes 1 class: okio.ByteString (implies api).
            
            Source: test
            ------------
            (no usages)'''.stripIndent()
    )

    when:
    result = build(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:okio:3.0.0')

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
            ------------------------------------------------------------
            You asked about the dependency 'com.squareup.okio:okio:3.0.0'.
            You have been advised to add this dependency to 'api'.
            It matched a bundle rule: com.squareup.okio:okio:3.0.0 was substituted for com.squareup.okio:okio-jvm:3.0.0.
            ------------------------------------------------------------
            
            Shortest path from :consumer to com.squareup.okio:okio:3.0.0 for compileClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio:3.0.0 for runtimeClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio:3.0.0 for testCompileClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
            
            Shortest path from :consumer to com.squareup.okio:okio:3.0.0 for testRuntimeClasspath:
            :consumer
            \\--- :producer
                  \\--- com.squareup.okio:okio:3.0.0
            
            Source: main
            ------------
            (no usages)
            
            Source: test
            ------------
            (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "can request reason for test-fixtures capability (#gradleVersion)"() {
    given:
    def project = new ReasonProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      ':consumer:reason',
      '--id', ':producer',
      '--capability', 'test-fixtures',
    )

    then:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
            ------------------------------------------------------------
            You asked about the dependency ':producer', with the capability 'test-fixtures'.
            There is no advice regarding this dependency.
            ------------------------------------------------------------
            
            Shortest path from :consumer to :producer for compileClasspath:
            :consumer
            \\--- :producer (capabilities: [test-fixtures])
            
            Shortest path from :consumer to :producer for runtimeClasspath:
            :consumer
            \\--- :producer (capabilities: [test-fixtures])
            
            Shortest path from :consumer to :producer for testCompileClasspath:
            :consumer
            \\--- :producer (capabilities: [test-fixtures])
            
            Shortest path from :consumer to :producer for testRuntimeClasspath:
            :consumer
            \\--- :producer (capabilities: [test-fixtures])
            
            There is no path from :consumer to :producer for testFixturesCompileClasspath
            
            
            Shortest path from :consumer to :producer for testFixturesRuntimeClasspath:
            :consumer
            \\--- :producer (capabilities: [test-fixtures])
            
            Source: main
            ------------
            * Uses 1 class: com.example.producer.Simpsons (implies implementation).
            
            Source: test
            ------------
            (no usages)
            
            Source: testFixtures
            --------------------
            (no usages)'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  private static void outputMatchesForProject(BuildResult result, String id) {
    def lines = Colors.decolorize(result.output).readLines()
    def asked = lines.find { it.startsWith('You asked about') }
    def advised = lines.find { it.startsWith('You have been advised') }

    assertThat(asked).isEqualTo("You asked about the dependency '$id'.".toString())
    assertThat(advised).isEqualTo("You have been advised to remove this dependency from 'api'.")
  }
}
