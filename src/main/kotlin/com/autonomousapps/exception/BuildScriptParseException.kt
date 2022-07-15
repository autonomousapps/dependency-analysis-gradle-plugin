package com.autonomousapps.exception

class BuildScriptParseException private constructor(msg: String) : RuntimeException(msg) {

  internal companion object {
    fun withErrors(messages: List<String>): BuildScriptParseException {
      var i = 1
      val msg = messages.joinToString { "${i++}: $it" }
      return BuildScriptParseException(msg)
    }
  }
}
