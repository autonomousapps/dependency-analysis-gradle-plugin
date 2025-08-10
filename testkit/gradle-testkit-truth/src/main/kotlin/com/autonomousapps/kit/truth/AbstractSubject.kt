// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth

import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject

// We need to extend Subject because failWithActual() is protected in that class.
public abstract class AbstractSubject<T> internal constructor(
  failureMetadata: FailureMetadata,
  actual: T?
) : Subject(failureMetadata, actual) {

  internal fun assertNonNull(actual: T?, message: () -> String): T {
    if (actual == null) {
      failWithActual(Fact.simpleFact(message.invoke()))
    }
    return actual!!
  }

  internal fun assertNonNull(actual: T?, key: String, value: Any?): T {
    if (actual == null) {
      failWithActual(key, value)
    }
    return actual!!
  }
}
