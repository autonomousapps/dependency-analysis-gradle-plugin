package com.autonomousapps.internal.utils

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.*
import kotlin.collections.HashSet

internal inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
  return filterTo(HashSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterToOrderedSet(predicate: (T) -> Boolean): TreeSet<T> {
  return filterTo(TreeSet(), predicate)
}

internal fun <T> Iterable<T>.filterNoneMatchingSorted(unwanted: Iterable<T>): TreeSet<T> {
  return filterToOrderedSet { a ->
    unwanted.none { b ->
      a == b
    }
  }
}

internal inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): HashSet<R> {
  return mapTo(HashSet(collectionSizeOrDefault(10)), transform)
}

internal inline fun <T, R> Iterable<T>.mapToOrderedSet(transform: (T) -> R): TreeSet<R> {
  return mapTo(TreeSet(), transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToSet(transform: (T) -> Iterable<R>): HashSet<R> {
  return flatMapTo(HashSet(collectionSizeOrDefault(10)), transform)
}

internal fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default

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
