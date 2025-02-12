// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

internal class LexicographicIterableComparator<T : Comparable<T>> : Comparator<Iterable<T>> {
  override fun compare(left: Iterable<T>?, right: Iterable<T>?): Int {
    if (left === right) return 0
    if (left == null || right == null) return if (left == null) -1 else 1

    val leftIterator = left.iterator()
    val rightIterator = right.iterator()

    while (leftIterator.hasNext() && rightIterator.hasNext()) {
      val compareResult = leftIterator.next().compareTo(rightIterator.next())
      if (compareResult != 0) {
        return compareResult
      }
    }

    if (leftIterator.hasNext()) return 1
    if (rightIterator.hasNext()) return -1

    return 0
  }
}

internal class MapSetComparator<K : Comparable<K>, V : Comparable<V>> : Comparator<Map<K, Set<V>>> {
  override fun compare(left: Map<K, Set<V>>?, right: Map<K, Set<V>>?): Int {
    if (left === right) return 0
    if (left == null || right == null) return if (left == null) -1 else 1

    if (left.size > right.size) return 1
    if (right.size > left.size) return -1

    val leftIterator = left.iterator()
    val rightIterator = right.iterator()

    while (leftIterator.hasNext() && rightIterator.hasNext()) {
      val leftEntry = leftIterator.next()
      val rightEntry = rightIterator.next()

      // Compare keys first
      val keyCompareResult = leftEntry.key.compareTo(rightEntry.key)
      if (keyCompareResult != 0) {
        return keyCompareResult
      }

      // If keys match, compare values
      val leftValuesIterator = leftEntry.value.iterator()
      val rightValuesIterator = rightEntry.value.iterator()

      while (leftValuesIterator.hasNext() && rightValuesIterator.hasNext()) {
        val valueCompareResult = leftValuesIterator.next().compareTo(rightValuesIterator.next())
        if (valueCompareResult != 0) {
          return valueCompareResult
        }
      }
    }

    if (leftIterator.hasNext()) return 1
    if (rightIterator.hasNext()) return -1

    return 0
  }
}
