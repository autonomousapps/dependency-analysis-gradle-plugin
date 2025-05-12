// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.*

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CompileOnlySpec extends AbstractJvmSpec {

  def "compile-only candidates can be compileOnly (#gradleVersion)"() {
    given:
    def project = new CompileOnlyProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "annotations can be compileOnly (#gradleVersion)"() {
    given:
    def project = new CompileOnlyProject2()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "compileOnly file dependency should not be marked as transitive (#gradleVersion)"() {
    given:
    def project = new CompileOnlyJarProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':external:jar')
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "compileOnly is not propagated to testImplementation (#gradleVersion)"() {
    given:
    def project = new CompileOnlyTestImplementationProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', 'org.apache.commons:commons-collections4',
    )

    then: 'advice is correct'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and: 'reason makes sense'
    assertThat(result.output).contains('There is no advice regarding this dependency.')

    where:
    gradleVersion << gradleVersions()
  }

  // The plugin cannot decide if something that is required for compilation is only needed at compile time.
  // Currently, such dependencies produce no advice at all. In the future the plugin could:
  // - Give an advice if one of these dependencies can be removed completely
  //   https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/648
  // - Give an advice id a dependency could be moved between 'compileOnly' <-> 'compileOnlyApi'
  //   (in projects where 'compileOnlyApi' exists)
  def "no advices for compileOnly, compileOnlyApi and providedCompile (#gradleVersion)"() {
    given:
    def project = new WarTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  // somewhat related to the above spec
  def "does not advise moving dependency from compileClasspath to runtimeClasspath (#gradleVersion)"() {
    given:
    def project = CompileOnlyTransitiveProject.base()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "does not advise moving dependency from extraCompileClasspath to extraRuntimeClasspath (#gradleVersion)"() {
    given:
    def project = CompileOnlyTransitiveProject.usingFeature()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "does not advise moving included build dependency from compileClasspath to runtimeClasspath (#gradleVersion)"() {
    given:
    def project = CompileOnlyTransitiveProject.usingIncludedBuild()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  // verifying that `compileOnly(project(":direct"))` doesn't somehow "leak" to `testImplementation(project(":direct"))`
  // that is, we still get the advice to add `testImplementation(project(":transitive-impl"))` even though we should
  // NEVER get advice to add `implementation(project(":transitive-impl))`.
  def "runtimeClasspath protection doesn't leak into other source sets (#gradleVersion)"() {
    given:
    def project = CompileOnlyTransitiveProject.usingSeparateSourceSet()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }
}
