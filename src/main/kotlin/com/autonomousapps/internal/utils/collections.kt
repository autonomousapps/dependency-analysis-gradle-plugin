package com.autonomousapps.internal.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier
import org.w3c.dom.*
import java.util.Collections
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
    it.name.endsWith(".class") && it.name != "module-info.class"
  }
}

/** Filters a collection of [ZipEntry]s to contain only class files (and not the module-info.class file). */
internal fun Iterable<ZipEntry>.filterToSetOfClassFiles(): Set<ZipEntry> {
  return filterToSet {
    it.name.endsWith(".class") && it.name != "module-info.class"
  }
}

/** Filters a collection of [ZipEntry]s to contain only class files (and not the module-info.class file). */
internal fun Iterable<ZipEntry>.asSequenceOfClassFiles(): Sequence<ZipEntry> {
  return asSequence().filter {
    it.name.endsWith(".class") && it.name != "module-info.class"
  }
}

/**
 * Filters a [FileCollection] to contain only class files.
 */
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
  comparator: Comparator<T>, predicate: (T) -> Boolean
): Set<T> {
  return filterTo(TreeSet(comparator), predicate)
}

internal fun <T> T.intoSet(): Set<T> = Collections.singleton(this)
internal fun <T> T.intoMutableSet(): MutableSet<T> = HashSet<T>().apply { add(this@intoMutableSet) }

fun <T : Any> T?.toSetOrEmpty(): Set<T> =
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

internal inline fun <R> NodeList.mapToSet(transform: (Node) -> R): Set<R> {
  val destination = HashSet<R>(length)
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

internal fun <R> NamedNodeMap.map(transform: (Node) -> R): List<R> {
  val destination = ArrayList<R>()
  for (i in 0 until length) {
    destination.add(transform(item(i)))
  }
  return destination
}

internal fun <R> Iterable<NamedNodeMap>.flatMap(transform: (Node) -> R): List<R> {
  val destination = ArrayList<R>()

  for (it in this) {
    for (i in 0 until it.length) {
      destination.add(transform(it.item(i)))
    }
  }

  return destination
}

internal fun Document.attrs(): List<Pair<String, String>> {
  return getElementsByTagName("*")
    .map { it.attributes }
    // this flatMap looks redundant but isn't!
    .flatMap { it }
    .filterIsInstance<Attr>()
    .map { it.name to it.value }
}

internal fun Document.contentReferences(): Map<String, String> {
  return getElementsByTagName("*")
    .map { it.textContent }
    .filter { it.startsWith('@') }
    // placeholder value; meaningless.
    .associateBy { "DIRECT-REFERENCE" }
}

internal inline fun <T> Iterable<T>.mutPartitionOf(
  predicate1: (T) -> Boolean, predicate2: (T) -> Boolean
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

// standard `all` function returns true if collection is empty!
internal inline fun <T> Collection<T>.reallyAll(predicate: (T) -> Boolean): Boolean {
  if (isEmpty()) return false
  for (element in this) if (!predicate(element)) return false
  return true
}

internal fun <T> Set<T>.efficient(): Set<T> = when {
  isEmpty() -> emptySet()
  size == 1 -> Collections.singleton(first())
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
