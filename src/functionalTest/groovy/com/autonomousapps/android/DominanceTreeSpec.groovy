// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.DominanceTreeProject
import com.google.common.truth.Correspondence
import com.google.common.truth.Correspondence.BinaryPredicate
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DominanceTreeSpec extends AbstractAndroidSpec {

  // regex: ^.*(\d+\.?\d*)
  private static FLOAT = ~/[^:\d]?(\d+[.]?\d+)/

  //missing (2)   : 4364.27 KiB :app, \--- 1.73 KiB :lib
  //unexpected (2): 4363.98 KiB :app, \--- 1.44 KiB :lib
  private static BinaryPredicate P = { String actual, String expected ->
//    def actualMatcher = actual =~ FLOAT
//    actualMatcher.findAll()
    actual == expected
  }
  private static Correspondence fuzzyMatcher = Correspondence.from(P, "Fuzzy matcher")

  def "regex works"() {
    given:
    def app = "4364.27 KiB :app"
    def lib = "\\--- 1.73 KiB :lib"
    def lib2 = "\\--- 1.73 KiB (2.00 KiB) :lib"

    when:
    def appMatcher = app =~ FLOAT
    def libMatcher = lib =~ FLOAT
    def lib2Matcher = lib2 =~ FLOAT

    then:
    appMatcher.findAll()*.get(1) == ['4364.27']
    libMatcher.findAll()*.get(1) == ['1.73']
    lib2Matcher.findAll()*.get(1) == ['1.73', '2.00']

    and:
    Float.parseFloat(appMatcher.findAll()*.get(1)[0] as String) == 4364.27f
    Float.parseFloat(libMatcher.findAll()*.get(1)[0] as String) == 1.73f
    Float.parseFloat(lib2Matcher.findAll()*.get(1)[0] as String) == 1.73f
    Float.parseFloat(lib2Matcher.findAll()*.get(1)[1] as String) == 2.00f
  }

  def "can print dominance tree (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DominanceTreeProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'app:printDominatorTreeCompileDebug')

    then:
    assertThat(project.actualTree())
      .comparingElementsUsing(fuzzyMatcher)
      .containsExactlyElementsIn(project.expectedTree).inOrder()

    where:
    [gradleVersion, agpVersion] << [[GRADLE_8_4, AGP_8_3.version]]
  }
}
