package com.autonomousapps.android

import com.autonomousapps.android.projects.DominanceTreeProject
import com.google.common.truth.Correspondence
import com.google.common.truth.Correspondence.BinaryPredicate
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DominanceTreeSpec extends AbstractAndroidSpec {

  // regex: ^.*(\d+\.?\d*)
//  private static FLOAT = ~/[^:]?(\d+[.]?\d+)/

  //missing (2)   : 4364.27 KiB :app, \--- 1.73 KiB :lib
  //unexpected (2): 4363.98 KiB :app, \--- 1.44 KiB :lib
  private static BinaryPredicate P = { String actual, String expected ->
    // TODO regex is a huge pain in the ass
//    def actualMatcher = actual =~ FLOAT
//    println actualMatcher.findAll()
    actual == expected
  }
  private static Correspondence fuzzyMatcher = Correspondence.from(P, "Fuzzy matcher")

  def "can print dominance tree (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DominanceTreeProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'app:printDominatorTreeDebug')

    then:
    assertThat(project.actualTree())
      .comparingElementsUsing(fuzzyMatcher)
      .containsExactlyElementsIn(project.expectedTree).inOrder()

    where:
    [gradleVersion, agpVersion] << [[GRADLE_7_5, AGP_7_3.version]]
  }
}
