// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

internal fun <T : Any> ListProperty<T>.addAll(elements: Iterable<Provider<out T>>) {
  elements.forEach { add(it) }
}
