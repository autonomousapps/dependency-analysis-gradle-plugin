// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.extension.Fail
import com.autonomousapps.extension.Undefined
import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.ModuleAdvice
import kotlin.Pair
import spock.lang.Specification

final class SeverityHandlerSpec extends Specification {

  private def fail = new Pair(new Fail(), new Undefined())

  def "should not fail on module advice when it's not actionable"() {
    given:
    Set<ModuleAdvice> moduleAdvice = [new AndroidScore(
      true, true, true, true, true
    )]
    def severityHandler = new SeverityHandler(
      fail, fail, fail, fail, fail, fail, fail, new Fail(), new Fail()
    )

    expect:
    !severityHandler.shouldFailModuleStructure(moduleAdvice)
  }

  def "should fail on module advice when it is not actionable"() {
    given:
    Set<ModuleAdvice> moduleAdvice = [new AndroidScore(
      false, false, false, false, false
    )]
    def severityHandler = new SeverityHandler(
      fail, fail, fail, fail, fail, fail, fail, new Fail(), new Fail()
    )

    expect:
    severityHandler.shouldFailModuleStructure(moduleAdvice)
  }
}
