package com.autonomousapps.internal.android

import spock.lang.Specification

final class AgpVersionSpec extends Specification {

  def "isSupported #expectedSupported for AGP #agpVersion"() {
    given:
    def version = AgpVersion.version(agpVersion)

    expect:
    version.isSupported() == expectedSupported

    where:
    agpVersion      | expectedSupported
    '4.2.0-alpha12' | false
    '4.2.0-beta04'  | false
    '4.2.1'         | false
    '4.2.2'         | true
    '7.0.0'         | true
    '7.1.3'         | true
    '7.3.2'         | true
    '7.4.1'         | true
    '7.4.2'         | false
    '8.0.0-beta01'  | false
    '8.1.0-alpha06' | false
  }
}
