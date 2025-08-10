// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Specification

final class AgpVersionSpec extends Specification {

  def "AgpVersion recognizes alpha qualifiers"() {
    given:
    def alpha2 = AgpVersion.version("4.0.0-alpha02")
    def alpha4 = AgpVersion.version("4.0.0-alpha04")

    expect:
    alpha4 > alpha2
  }

  def "AgpVersion recognizes rc qualifiers"() {
    given:
    def alpha2 = AgpVersion.version("4.0.0-alpha02")
    def rc1 = AgpVersion.version("4.0.0-rc01")

    expect:
    rc1 > alpha2
  }
}
