// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.android

import org.gradle.api.Project
import spock.lang.Specification

final class AndroidGradlePluginFactorySpec extends Specification {

  def "returns #expectedType for AGP #agpVersion"() {
    given:
    def stubProject = Stub(Project)
    def factory = new AndroidGradlePluginFactory(stubProject, agpVersion)

    expect:
    factory.newAdapter().class.is(expectedType)

    where:
    agpVersion      | expectedType
    '4.2.0-alpha12' | AndroidGradlePlugin4_2
    '4.2.0-beta04'  | AndroidGradlePlugin4_2
    '4.2.0'         | AndroidGradlePlugin4_2
    '4.2.2'         | AndroidGradlePlugin4_2
    '7.0.0'         | AndroidGradlePlugin4_2
    '7.1.0'         | AndroidGradlePlugin4_2
  }
}
