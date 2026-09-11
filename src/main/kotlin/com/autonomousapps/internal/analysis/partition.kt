// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analysis

// TODO(tsr): move to collections.kt?
internal fun <T, V> List<T>.partitionBy(
  keySelector: (T) -> String,
  cache: (String) -> V?,
): Pair<Map<String, V>, List<T>> {
  val hits = linkedMapOf<String, V>()
  val misses = mutableListOf<T>()

  forEach { a ->
    val key = keySelector(a)
    val cached = cache(key)
    if (cached != null) {
      hits[key] = cached
    } else {
      misses += a
    }
  }

  return hits to misses
}
