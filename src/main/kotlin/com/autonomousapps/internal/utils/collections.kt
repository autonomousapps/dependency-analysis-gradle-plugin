package com.autonomousapps.internal.utils

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

internal inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
  return filterTo(HashSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterToOrderedSet(predicate: (T) -> Boolean): TreeSet<T> {
  return filterTo(TreeSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterToOrderedSet(
  comparator: Comparator<T>, predicate: (T) -> Boolean
): TreeSet<T> {
  return filterTo(TreeSet(comparator), predicate)
}

internal fun <T> Iterable<T>.filterNoneMatchingSorted(unwanted: Iterable<T>): TreeSet<T> {
  return filterToOrderedSet { a ->
    unwanted.none { b ->
      a == b
    }
  }
}

internal inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): LinkedHashSet<R> {
  return mapTo(LinkedHashSet(collectionSizeOrDefault(10)), transform)
}

internal inline fun <T, R> Iterable<T>.mapToOrderedSet(transform: (T) -> R): TreeSet<R> {
  return mapTo(TreeSet(), transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToSet(transform: (T) -> Iterable<R>): HashSet<R> {
  return flatMapTo(HashSet(collectionSizeOrDefault(10)), transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToOrderedSet(transform: (T) -> Iterable<R>): TreeSet<R> {
  return flatMapTo(TreeSet(), transform)
}

internal fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default

internal inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): HashSet<R> {
  return mapNotNullTo(HashSet(), transform)
}

internal inline fun <T, R : Any> Iterable<T>.mapNotNullToOrderedSet(transform: (T) -> R?): TreeSet<R> {
  return mapNotNullTo(TreeSet(), transform)
}

internal fun <T> Iterable<Iterable<T>>.flattenToSet(): Set<T> {
  val result = HashSet<T>()
  for (element in this) {
    result.addAll(element)
  }
  return result
}

internal inline fun <R> NodeList.mapNotNull(transform: (Node) -> R?): List<R> {
  val destination = ArrayList<R>(length)
  for (i in 0 until length) {
    transform(item(i))?.let { destination.add(it) }
  }
  return destination
}

internal inline fun <R> NodeList.map(transform: (Node) -> R): List<R> {
  val destination = ArrayList<R>(length)
  for (i in 0 until length) {
    destination.add(transform(item(i)))
  }
  return destination
}

internal inline fun NodeList.filter(predicate: (Node) -> Boolean): List<Node> {
  val destination = ArrayList<Node>(length)
  for (i in 0 until length) {
    if (predicate(item(i))) destination.add(item(i))
  }
  return destination
}

internal inline fun NodeList.filterToSet(predicate: (Node) -> Boolean): Set<Node> {
  val destination = LinkedHashSet<Node>(length)
  for (i in 0 until length) {
    if (predicate(item(i))) destination.add(item(i))
  }
  return destination
}

// standard `all` function returns true if collection is empty!
internal inline fun <T> Collection<T>.reallyAll(predicate: (T) -> Boolean): Boolean {
  if (isEmpty()) return false
  for (element in this) if (!predicate(element)) return false
  return true
}

internal fun <T> Set<T>.efficient(): Set<T> {
  return when {
    isEmpty() -> emptySet()
    size == 1 -> Collections.singleton(first())
    else -> this
  }
}
