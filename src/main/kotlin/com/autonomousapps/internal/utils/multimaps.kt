// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap

internal fun <K, V> emptySetMultimap(): SetMultimap<K, V> = Multimaps.unmodifiableSetMultimap(
  MultimapBuilder.hashKeys().hashSetValues().build<K, V>()
)

internal fun <K, V : Comparable<V>> newSetMultimap(): SetMultimap<K, V> {
  return MultimapBuilder.hashKeys().treeSetValues().build()
}
