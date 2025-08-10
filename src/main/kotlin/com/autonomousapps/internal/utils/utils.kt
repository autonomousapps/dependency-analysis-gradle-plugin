// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import okio.source
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File
import java.util.*

/**
 * Deletes all the files in this directory (but not the subdirectories).
 *
 * TODO: delete the subdirectories too, but avoid deleting the top-level directory.
 */
internal fun DirectoryProperty.delete(): DirectoryProperty {
  get().asFileTree.visit {
    if (!isDirectory) {
      file.delete()
    }
  }

  return this
}

/**
 * Resolves the file from the property and deletes its contents, then returns the file.
 */
internal fun RegularFileProperty.getAndDelete(): File {
  val file = get().asFile
  file.delete()
  return file
}

/**
 * Resolves the file from the provider and deletes its contents, then returns the file.
 */
internal fun Provider<RegularFile>.getAndDelete(): File {
  val file = get().asFile
  file.delete()
  return file
}

/**
 * Buffer reads of the nullable RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.fromNullableJsonSet(): Set<T> {
  return orNull?.fromJsonSet() ?: emptySet()
}

/**
 * Buffers reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.fromJsonSet(
  compressed: Boolean = false,
): Set<T> = get().fromJsonSet(compressed)

/**
 * Buffers reads of the RegularFile from disk to the set.
 */
internal inline fun <reified T> RegularFile.fromJsonSet(
  compressed: Boolean = false,
): Set<T> {
  val source = if (compressed) {
    GzipSource(asFile.source()).buffer()
  } else {
    asFile.bufferRead()
  }

  return source.use { getJsonSetAdapter<T>().fromJson(it)!! }
}

/**
 * Buffers reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.fromJsonList(): List<T> = get().fromJsonList()

/**
 * Buffers reads of the RegularFile from disk to the set.
 */
internal inline fun <reified T> RegularFile.fromJsonList(): List<T> {
  return asFile.bufferRead().use { reader ->
    getJsonListAdapter<T>().fromJson(reader)!!
  }
}

internal inline fun <reified K, reified V> RegularFileProperty.fromJsonMapList(): Map<K, List<V>> {
  return get().fromJsonMapList()
}

internal inline fun <reified K, reified V> RegularFileProperty.fromJsonMapSet(): Map<K, Set<V>> {
  return get().fromJsonMapSet()
}

internal inline fun <reified K, reified V> RegularFile.fromJsonMapList(): Map<K, List<V>> {
  return asFile.fromJsonMapList()
}

internal inline fun <reified K, reified V> RegularFile.fromJsonMapSet(): Map<K, Set<V>> {
  return asFile.fromJsonMapSet()
}

internal inline fun <reified K, reified V> File.fromJsonMapList(): Map<K, List<V>> {
  return bufferRead().fromJsonMapList()
}

internal inline fun <reified K, reified V> File.fromJsonMapSet(): Map<K, Set<V>> {
  return bufferRead().fromJsonMapSet()
}

/**
 * Buffers reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified K, reified V> RegularFileProperty.fromJsonMap(): Map<K, V> = get().fromJsonMap()

/**
 * Buffers reads of the RegularFile from disk to the set.
 */
internal inline fun <reified K, reified V> RegularFile.fromJsonMap(): Map<K, V> = asFile.fromJsonMap()

/**
 * Buffers reads of the File from disk to the set.
 */
internal inline fun <reified K, reified V> File.fromJsonMap(): Map<K, V> {
  return bufferRead().use { reader ->
    getJsonMapAdapter<K, V>().fromJson(reader)!!
  }
}

/**
 * Buffer reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.fromJson(): T = get().fromJson()

/**
 * Buffer reads of the RegularFile from disk to the set.
 */
internal inline fun <reified T> RegularFile.fromJson(): T = asFile.fromJson()

/**
 * Buffer reads of the File from disk to the set.
 */
internal inline fun <reified T> File.fromJson(): T {
  return bufferRead().use { reader ->
    getJsonAdapter<T>().fromJson(reader)!!
  }
}

internal fun RegularFileProperty.readLines(): List<String> = get().readLines()

internal fun RegularFile.readLines(): List<String> = asFile.readLines()

internal fun RegularFileProperty.readText(): String = get().asFile.readText()

private fun File.bufferRead(): BufferedSource = source().buffer()

internal fun String.capitalizeSafely(): String {
  return replaceFirstChar(Char::uppercase)
}

// copied from StringsJVM.kt
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal fun String.lowercase(): String = (this as java.lang.String).toLowerCase(Locale.ROOT)

// Print dependency tree (like running the `dependencies` task).
@Suppress("unused")
internal fun printDependencyTree(dependencies: Set<DependencyResult>, level: Int = 0) {
  dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { result ->
    val resolvedComponentResult = result.selected
    println("${"  ".repeat(level)}- ${resolvedComponentResult.id}")
    printDependencyTree(resolvedComponentResult.dependencies, level + 1)
  }
}
