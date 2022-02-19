package com.autonomousapps.internal.utils

internal class LexicographicIterableComparator<T : Comparable<T>> : Comparator<Iterable<T>> {
  override fun compare(left: Iterable<T>?, right: Iterable<T>?): Int {
    if (left === right) {
      return 0
    }

    if (left == null || right == null) {
      return if (left == null) -1 else 1
    }

    val leftIterator = left.iterator()
    val rightIterator = right.iterator()

    while (leftIterator.hasNext() && rightIterator.hasNext()) {
      val leftElement = leftIterator.next()
      val rightElement = rightIterator.next()
      val compareResult = leftElement.compareTo(rightElement)

      if (compareResult != 0) {
        return compareResult
      }
    }

    val leftHasNext = if (leftIterator.hasNext()) 1 else 0
    val rightHasNext = if (rightIterator.hasNext()) 1 else 0

    return leftHasNext - rightHasNext
  }
}
