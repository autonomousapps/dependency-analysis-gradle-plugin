// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.internal.antlr.v4.runtime.ANTLRErrorListener
import com.autonomousapps.internal.antlr.v4.runtime.Parser
import com.autonomousapps.internal.antlr.v4.runtime.RecognitionException
import com.autonomousapps.internal.antlr.v4.runtime.Recognizer
import com.autonomousapps.internal.antlr.v4.runtime.atn.ATNConfigSet
import com.autonomousapps.internal.antlr.v4.runtime.dfa.DFA
import java.util.*

internal abstract class AbstractErrorListener : ANTLRErrorListener {

  override fun syntaxError(
    recognizer: Recognizer<*, *>,
    offendingSymbol: Any,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
  }

  override fun reportAmbiguity(
    recognizer: Parser,
    dfa: DFA,
    startIndex: Int,
    stopIndex: Int,
    exact: Boolean,
    ambigAlts: BitSet,
    configs: ATNConfigSet
  ) {
  }

  override fun reportAttemptingFullContext(
    recognizer: Parser,
    dfa: DFA,
    startIndex: Int,
    stopIndex: Int,
    conflictingAlts: BitSet,
    configs: ATNConfigSet
  ) {
  }

  override fun reportContextSensitivity(
    recognizer: Parser,
    dfa: DFA,
    startIndex: Int,
    stopIndex: Int,
    prediction: Int,
    configs: ATNConfigSet
  ) {
  }
}
