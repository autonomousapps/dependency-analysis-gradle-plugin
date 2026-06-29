// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.*
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.PendingFeature

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.GradleBuilder.build
import static com.autonomousapps.kit.GradleBuilder.buildAndFail
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class ResSpec extends AbstractAndroidSpec {

  def "plugin accounts for android resource usage (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ResProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth', ':app:reason', '--id', 'androidx.lifecycle:lifecycle-viewmodel:')

    then:
    result.task(':buildHealth').outcome == TaskOutcome.SUCCESS

    and:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "detects attr usage in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AttrResProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "detects res usage in menu.xml file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidMenuProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "gracefully handles @null in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AttrResWithNullProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "gracefully handles dataBinding expressions in res files (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingWithExpressionsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1211
  def "gracefully handles empty res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new EmptyResFile(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "detects content reference in res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DrawableFileProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "detects theme set in manifest (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThemeProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }

  def "detects theme set in manifest on Activity (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThemeActivityProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }

  def "support question mark character on android:text in layout res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTextQuestionMarkProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "doesn't suggest adding dependency just because it has matching attribute (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ResDuplicateAttrProject(agpVersion)

    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  // TODO(tsr): this test case demonstrates (or more accurately: fails to demonstrate) any issue when multiple Android
  //  libraries provide a resource with the same name, and the consumer side only references that resource in an XML
  //  file (so there's no qualified reference in code for more accurate detection).
  // See e.g. https://developer.android.com/studio/projects/android-library#Considerations for information on resource
  //  merging; and
  // https://developer.android.com/reference/tools/gradle-api/8.3/null/com/android/build/api/dsl/CommonExtension#setResourcePrefix(kotlin.String)
  //  for an AGP DSL that can help mitigate the problem (set unique prefixes everywhere and use them).
  // This is annotated with `@PendingFeature` because the assertion is that the build fails (but the build succeeds). If
  // there is ever any improvement in this scenario, this case will begin to really fail and then we can update the
  // assertions.
  @PendingFeature(reason = "https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1731")
  def "handles duplicate res-by-res producers (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ResByResProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')
    // The output is identical
//    build(gradleVersion, gradleProject.rootDir, ':app:reason', '--id', ':res1')
//    build(gradleVersion, gradleProject.rootDir, ':app:reason', '--id', ':res2')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
