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
    '3.5'           | AndroidGradlePlugin3_5
    '3.5.3'         | AndroidGradlePlugin3_5
    '3.5.4'         | AndroidGradlePlugin3_5
    '3.6'           | AndroidGradlePlugin3_6
    '3.6.3'         | AndroidGradlePlugin3_6
    '4.0'           | AndroidGradlePlugin4_0
    '4.0.0'         | AndroidGradlePlugin4_0
    '4.0.0-rc01'    | AndroidGradlePlugin4_0
    '4.0.1'         | AndroidGradlePlugin4_0
    '4.1'           | AndroidGradlePlugin4_1
    '4.1.0'         | AndroidGradlePlugin4_1
    '4.1.0-alpha09' | AndroidGradlePlugin4_1
    '4.1.0-beta01'  | AndroidGradlePlugin4_1
    '4.1.0-rc03'    | AndroidGradlePlugin4_1
    '4.1.2'         | AndroidGradlePlugin4_1
    '4.2.0-alpha12' | AndroidGradlePlugin4_2
    '4.2.0-beta04'  | AndroidGradlePlugin4_2
  }
}
