// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.internal.antlr.v4.runtime.Token
import com.autonomousapps.internal.cash.grammar.kotlindsl.parse.Rewriter

internal class TrackingRewriter(private val rewriter: Rewriter) {

  var hasChanges: Boolean = false
    private set

  val text: String get() = rewriter.text

  fun delete(index: Token) {
    rewriter.delete(index)
    hasChanges = true
  }

  fun delete(start: Token, stop: Token) {
    rewriter.delete(start, stop)
    hasChanges = true
  }

  fun deleteNewlineToRight(after: Token) {
    rewriter.deleteNewlineToRight(after)
    hasChanges = true
  }

  fun deleteWhitespaceToLeft(before: Token) {
    rewriter.deleteWhitespaceToLeft(before)
    hasChanges = true
  }

  fun insertBefore(token: Token, text: Any) {
    rewriter.insertBefore(token, text)
    hasChanges = true
  }

  fun replace(from: Token, to: Token, text: Any?) {
    rewriter.replace(from, to, text)
    hasChanges = true
  }
}
