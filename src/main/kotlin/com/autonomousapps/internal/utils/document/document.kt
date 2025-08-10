// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.document

import org.w3c.dom.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.associateBy
import kotlin.collections.filter
import kotlin.collections.filterIsInstance
import kotlin.collections.map

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
