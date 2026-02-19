// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AdvancedReflectionProject
import com.autonomousapps.jvm.projects.ImplRuntimeTestImplConfusionProject
import com.autonomousapps.jvm.projects.ReflectionProject
import com.autonomousapps.jvm.projects.TransitiveRuntimeProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RuntimeOnlySpec extends AbstractJvmSpec {

  def "don't suggest implementation to runtimeOnly when used for testImplementation (#gradleVersion)"() {
    given:
    def project = new ImplRuntimeTestImplConfusionProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', ImplRuntimeTestImplConfusionProject.SPARK_SQL_ID,
    )

    then: 'advice is correct'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and: 'reason makes sense and supports multiple pieces of advice for the same dependency'
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
      ------------------------------------------------------------
      You asked about the dependency 'org.apache.spark:spark-sql_2.12:3.5.0'.
      You have been advised to add this dependency to 'testImplementation'.
      You have been advised to change this dependency to 'runtimeOnly' from 'implementation'.
      ------------------------------------------------------------'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "transitive dependencies with runtime capabilities are added directly (#gradleVersion)"() {
    given:
    def project = new TransitiveRuntimeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "detects Class forName (#gradleVersion)"() {
    given:
    def project = new ReflectionProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion,
      gradleProject.rootDir,
      'buildHealth',
      ':consumer:reason', '--id', ':direct'
    )

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)
    assertThat(Colors.decolorize(result.output)).contains('* Accessed 3 times by reflection: (1) the-project:uses-reflection in class com.example.reflection.UsesReflection: com.example.direct.Direct, (2) the-project:uses-reflection in class com.example.reflection.UsesReflection: com.example.direct.Direct$Inner, (3) the-project:uses-reflection in class com.example.reflection.UsesReflection: com.example.direct.Direct$StaticInner (implies runtimeOnly).')

    where:
    gradleVersion << gradleVersions()
  }

  def "detects advanced uses of Class forName (#gradleVersion)"() {
    given:
    def project = new AdvancedReflectionProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    def variant = project.actualExplodedJarsForProjectAndVariant(":aggregator", "main")
    Map<String, Map<String, Set<String>>> reflectiveAccesses = variant.collectEntries { [it.coordinates.identifier, it.getReflectiveAccesses()] }

    then:
    assertThat(reflectiveAccesses).containsExactly(
      "the-project:class-lookup", [:],
      "the-project:utils", [:],
      "the-project:framework-like-spring",
      [
        "framework.like.spring.CheckForOptionalDependency"             : ["optional.dependency.OptionalDependency"] as Set,
        "framework.like.spring.CheckForOptionalDependencyUsingConstant": ["optional.dependency.OptionalDependency"] as Set
      ],
      "the-project:optional-dependency", [:],
    )
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
