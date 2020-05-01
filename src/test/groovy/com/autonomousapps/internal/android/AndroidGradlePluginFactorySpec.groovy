package com.autonomousapps.internal.android

import org.gradle.api.Project
import spock.lang.Specification
import spock.lang.Unroll

class AndroidGradlePluginFactorySpec extends Specification {

  @Unroll
  def "returns #expectedType for AGP #agpVersion"() {
    given:
    def stubProject = Stub(Project)
    def factory = new AndroidGradlePluginFactory(stubProject, agpVersion)

    expect:
    factory.newAdapter().class.is(expectedType)

    where:
    agpVersion      | expectedType
    "3.5"           | AndroidGradlePlugin3_5
    "3.5.3"         | AndroidGradlePlugin3_5
    "3.6"           | AndroidGradlePlugin3_6
    "3.6.3"         | AndroidGradlePlugin3_6
    "4.0"           | AndroidGradlePlugin4_0
    "4.0.0"         | AndroidGradlePlugin4_0
    "4.0.0-beta05"  | AndroidGradlePlugin4_0
    "4.1"           | AndroidGradlePlugin4_1
    "4.1.0"         | AndroidGradlePlugin4_1
    "4.1.0-alpha08" | AndroidGradlePlugin4_1
  }
}
