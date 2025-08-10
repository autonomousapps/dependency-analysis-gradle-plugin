// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.exception

public class BuildScriptParseException private constructor(msg: String) : RuntimeException(msg) {

  internal companion object {
    fun withErrors(messages: List<String>): BuildScriptParseException {
      var i = 1
      val msg = messages.joinToString { "${i++}: $it" }
      return BuildScriptParseException(msg)
    }
  }
}
