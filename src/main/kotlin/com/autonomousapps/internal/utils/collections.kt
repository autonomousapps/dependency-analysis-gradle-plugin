// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier
import java.io.File
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Takes an [ArtifactCollection] and filters out all [OpaqueComponentIdentifier]s, which seem to be jars from the Gradle
 * distribution, e.g. "Gradle API", "Gradle TestKit", and "Gradle Kotlin DSL". They are often not very useful for
 * analysis.
 */
internal fun ArtifactCollection.filterNonGradle(): List<ResolvedArtifactResult> = filterNot {
  // e.g. "Gradle API", "Gradle TestKit", "Gradle Kotlin DSL"
  it.id.componentIdentifier is OpaqueComponentIdentifier
}

internal fun Sequence<ResolvedArtifactResult>.filterNonGradle() = filterNot {
  // e.g. "Gradle API", "Gradle TestKit", "Gradle Kotlin DSL"
  it.id.componentIdentifier is OpaqueComponentIdentifier
}

/**
 * Transforms a [ZipFile] into a collection of [ZipEntry]s, which contains only class files (and not
 * the module-info.class file).
 */
internal fun ZipFile.asClassFiles(): Set<ZipEntry> {
  return entries().toList().filterToSetOfClassFiles()
}

internal fun ZipFile.asSequenceOfClassFiles(): Sequence<ZipEntry> {
  return entries().asSequence().filter {
    it.name.endsWith(".class") && !it.name.endsWith("module-info.class")
  }
}

/** Filters a collection of [ZipEntry]s to contain only class files (and not the module-info.class file). */
internal fun Iterable<ZipEntry>.filterToSetOfClassFiles(): Set<ZipEntry> {
  return filterToSet {
    it.name.endsWith(".class") && !it.name.endsWith("module-info.class")
  }
}

/** Filters a collection of [ZipEntry]s to contain only class files (and not the module-info.class file). */
internal fun Iterable<ZipEntry>.asSequenceOfClassFiles(): Sequence<ZipEntry> {
  return asSequence().filter {
    it.name.endsWith(".class") && !it.name.endsWith("module-info.class")
  }
}

// Can't use Iterable<File> because of signature clash with Iterable<ZipEntry> above.
internal fun Collection<File>.asSequenceOfClassFiles(): Sequence<File> {
  return asSequence().filter { it.extension == "class" && !it.name.endsWith("module-info.class") }
}

internal fun Iterable<File>.filterToClassFiles(): List<File> {
  return filter { it.extension == "class" && !it.name.endsWith("module-info.class") }
}

/** Filters a [FileCollection] to contain only class files. */
internal fun FileCollection.filterToClassFiles(): FileCollection {
  return filter {
    it.isFile && it.name.endsWith(".class")
  }
}

internal inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> {
  return filterTo(HashSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterNotToSet(predicate: (T) -> Boolean): Set<T> {
  return filterNotTo(HashSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterToOrderedSet(predicate: (T) -> Boolean): Set<T> {
  return filterTo(TreeSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterNotToOrderedSet(predicate: (T) -> Boolean): Set<T> {
  return filterNotTo(TreeSet(), predicate)
}

internal inline fun <T> Iterable<T>.filterToOrderedSet(
  comparator: Comparator<T>, predicate: (T) -> Boolean,
): Set<T> {
  return filterTo(TreeSet(comparator), predicate)
}

internal inline fun <T, R> Iterable<T>?.mapToMutableList(transform: (T) -> R): MutableList<R> {
  return this?.mapTo(ArrayList(), transform) ?: mutableListOf()
}

internal fun <T> T.intoSet(): Set<T> = Collections.singleton(this)
internal fun <T> T.intoMutableSet(): MutableSet<T> = HashSet<T>().apply { add(this@intoMutableSet) }

internal fun <T : Any> T?.toSetOrEmpty(): Set<T> =
  if (this == null) emptySet() else setOf(this)

internal inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> {
  return mapTo(LinkedHashSet(collectionSizeOrDefault(10)), transform)
}

internal inline fun <T, R> Iterable<T>.mapToOrderedSet(transform: (T) -> R): Set<R> {
  return mapTo(TreeSet(), transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToSet(transform: (T) -> Iterable<R>): Set<R> {
  return flatMapToMutableSet(transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToMutableSet(transform: (T) -> Iterable<R>): MutableSet<R> {
  return flatMapTo(HashSet(collectionSizeOrDefault(10)), transform)
}

internal inline fun <T, R> Iterable<T>.flatMapToOrderedSet(transform: (T) -> Iterable<R>): Set<R> {
  return flatMapTo(TreeSet(), transform)
}

internal fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int =
  if (this is Collection<*>) this.size
  else default

internal inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> {
  return mapNotNullTo(HashSet(), transform)
}

internal inline fun <T, R : Any> Iterable<T>.mapNotNullToOrderedSet(transform: (T) -> R?): Set<R> {
  return mapNotNullTo(TreeSet(), transform)
}

/**
 * Sort elements keeping Comparable-equal elements (stable sorting).
 * This method has different semantics with standard toSortedSet(Comparator).
 */
internal fun <T> Collection<T>.softSortedSet(comparator: Comparator<in T>): Set<T> {
  val list = ArrayList(this)
  list.sortWith(comparator)
  return LinkedHashSet(list)
}

internal inline fun <T> Iterable<T>.mutPartitionOf(
  predicate1: (T) -> Boolean,
  predicate2: (T) -> Boolean,
): Pair<MutableSet<T>, MutableSet<T>> {
  val first = LinkedHashSet<T>()
  val second = LinkedHashSet<T>()
  for (element in this) {
    if (predicate1(element)) {
      first.add(element)
    } else if (predicate2(element)) {
      second.add(element)
    }
  }
  return Pair(first, second)
}

internal inline fun <T> Iterable<T>.mutPartitionOf(
  predicate1: (T) -> Boolean,
  predicate2: (T) -> Boolean,
  predicate3: (T) -> Boolean,
): Triple<MutableSet<T>, MutableSet<T>, MutableSet<T>> {
  val first = LinkedHashSet<T>()
  val second = LinkedHashSet<T>()
  val third = LinkedHashSet<T>()
  for (element in this) {
    if (predicate1(element)) {
      first.add(element)
    } else if (predicate2(element)) {
      second.add(element)
    } else if (predicate3(element)) {
      third.add(element)
    }
  }
  return Triple(first, second, third)
}

internal inline fun <T> Iterable<T>.mutPartitionOf(
  predicate1: (T) -> Boolean,
  predicate2: (T) -> Boolean,
  predicate3: (T) -> Boolean,
  predicate4: (T) -> Boolean,
): Quadruple<MutableSet<T>, MutableSet<T>, MutableSet<T>, MutableSet<T>> {
  val first = LinkedHashSet<T>()
  val second = LinkedHashSet<T>()
  val third = LinkedHashSet<T>()
  val fourth = LinkedHashSet<T>()
  for (element in this) {
    if (predicate1(element)) {
      first.add(element)
    } else if (predicate2(element)) {
      second.add(element)
    } else if (predicate3(element)) {
      third.add(element)
    } else if (predicate4(element)) {
      fourth.add(element)
    }
  }
  return Quadruple(first, second, third, fourth)
}

internal inline fun <T> Iterable<T>.mutPartitionOf(
  predicate1: (T) -> Boolean,
  predicate2: (T) -> Boolean,
  predicate3: (T) -> Boolean,
  predicate4: (T) -> Boolean,
  predicate5: (T) -> Boolean,
): Pentuple<MutableSet<T>, MutableSet<T>, MutableSet<T>, MutableSet<T>, MutableSet<T>> {
  val first = LinkedHashSet<T>()
  val second = LinkedHashSet<T>()
  val third = LinkedHashSet<T>()
  val fourth = LinkedHashSet<T>()
  val fifth = LinkedHashSet<T>()
  for (element in this) {
    if (predicate1(element)) {
      first.add(element)
    } else if (predicate2(element)) {
      second.add(element)
    } else if (predicate3(element)) {
      third.add(element)
    } else if (predicate4(element)) {
      fourth.add(element)
    } else if (predicate5(element)) {
      fifth.add(element)
    }
  }
  return Pentuple(first, second, third, fourth, fifth)
}

internal data class Quadruple<out A, out B, out C, out D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
) {
  override fun toString(): String = "($first, $second, $third, $fourth)"
}

internal data class Pentuple<out A, out B, out C, out D, out F>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: F,
) {
  override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

// standard `all` function returns true if collection is empty!
internal inline fun <T> Collection<T>.reallyAll(predicate: (T) -> Boolean): Boolean {
  if (isEmpty()) return false
  for (element in this) if (!predicate(element)) return false
  return true
}

internal fun <T> List<T>.efficient(): List<T> = when {
  isEmpty() -> emptyList()
  size == 1 -> Collections.singletonList(first())
  else -> this
}

internal fun <T> Set<T>.efficient(): Set<T> = when {
  isEmpty() -> emptySet()
  size == 1 -> Collections.singleton(first())
  else -> this
}

internal fun <K, V> Map<K, V>.efficient(): Map<K, V> = when {
  isEmpty() -> emptyMap()
  size == 1 -> Collections.singletonMap(keys.first(), values.first())
  else -> this
}

/**
 * Given a list of pairs, where the pairs are key -> (value as Set) pairs, merge into a map (not
 * losing any values).
 */
internal fun <T, U> List<Pair<T, MutableSet<U>>>.mergedMapSets(): Map<T, Set<U>> {
  return foldRight(linkedMapOf<T, MutableSet<U>>()) { (key, values), map ->
    map.apply {
      merge(key, values) { old, new -> old.apply { addAll(new) } }
    }
  }
}

internal inline fun <reified K, reified V> Map<K, Set<V>>.mergeWith(other: Map<K, Set<V>>): Map<K, Set<V>>
  where K : Comparable<K>, V : Comparable<V> {

  val merged = sortedMapOf<K, SortedSet<V>>()

  forEach { k, v ->
    merged.put(k, v.toSortedSet())
  }
  other.forEach { k, v ->
    merged.merge(k, v.toSortedSet()) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }

  return merged
}

internal inline fun <C> C.ifNotEmpty(block: (C) -> Unit) where C : Collection<*> {
  if (isNotEmpty()) {
    block(this)
  }
}

internal inline fun <K, V> Map<K, V>.ifNotEmpty(block: (Map<K, V>) -> Unit) {
  if (isNotEmpty()) {
    block(this)
  }
}

internal fun <K, V> Map<V, K>.reversed() = entries.associateBy({ it.value }) { it.key }
