// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.fixtures.AndroidConstantsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ConstantsSpec extends AbstractAndroidSpec {

  def "finds constants in android-kotlin projects (#gradleVersion AGP #agpVersion)"() {
    given:
    androidProject = AndroidConstantsProject.androidProjectThatUsesConstants(agpVersion as String)

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualUnusedDependencies = androidProject.adviceFor('app')
    assertThat(actualUnusedDependencies).isEmpty()

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
