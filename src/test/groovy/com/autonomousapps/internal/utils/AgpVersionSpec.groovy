package com.autonomousapps.internal.utils

import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Specification

class AgpVersionSpec extends Specification {

  def "AgpVersion recognizes alpha qualifiers"() {
    given:
    def alpha2 = AgpVersion.version("4.0.0-alpha02")
    def alpha4 = AgpVersion.version("4.0.0-alpha04")

    expect:
    alpha4 > alpha2
  }
}
