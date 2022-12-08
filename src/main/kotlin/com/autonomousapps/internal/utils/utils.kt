@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import okio.buffer
import okio.source
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import java.io.File
import java.util.*

/**
 * Resolves the file from the property and deletes its contents, then returns the file.
 */
internal fun RegularFileProperty.getAndDelete(): File {
  val file = get().asFile
  file.delete()
  return file
}

internal inline fun <reified T> RegularFileProperty.fromJsonSet(): Set<T> = get().fromJsonSet()
internal inline fun <reified T> RegularFile.fromJsonSet(): Set<T> = asFile.readText().fromJsonSet()

/**
 * Buffers reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.bufferReadJsonSet(): Set<T> = get().bufferReadJsonSet()

/**
 * Buffers reads of the RegularFile from disk to the set.
 */
internal inline fun <reified T> RegularFile.bufferReadJsonSet(): Set<T> {
  asFile.source().buffer().use { reader ->
    return getJsonSetAdapter<T>().fromJson(reader)!!
  }
}

internal inline fun <reified T> RegularFileProperty.fromJsonList(): List<T> = get().fromJsonList()
internal inline fun <reified T> RegularFile.fromJsonList(): List<T> = asFile.readText().fromJsonList()

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
  return readText().fromJsonMapList()
}

internal inline fun <reified K, reified V> File.fromJsonMapSet(): Map<K, Set<V>> {
  return readText().fromJsonMapSet()
}

internal inline fun <reified K, reified V> RegularFileProperty.fromJsonMap(): Map<K, V> = get().fromJsonMap()
internal inline fun <reified K, reified V> RegularFile.fromJsonMap(): Map<K, V> = asFile.fromJsonMap()
internal inline fun <reified K, reified V> File.fromJsonMap(): Map<K, V> = readText().fromJsonMap()

internal inline fun <reified T> RegularFileProperty.fromJson(): T = get().fromJson()
internal inline fun <reified T> RegularFile.fromJson(): T = asFile.fromJson()
internal inline fun <reified T> File.fromJson(): T = readText().fromJson()

internal inline fun <reified T> RegularFileProperty.fromNullableJsonSet(): Set<T>? {
  return orNull?.asFile?.readText()?.fromJsonSet()
}

/**
 * Buffer reads of the RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.bufferReadJson(): T = get().bufferReadJson()

/**
 * Buffer reads of the RegularFile from disk to the set.
 */
internal inline fun <reified T> RegularFile.bufferReadJson(): T {
  asFile.source().buffer().use { reader ->
    return getJsonAdapter<T>().fromJson(reader)!!
  }
}

/**
 * Buffer reads of the nullable RegularFileProperty from disk to the set.
 */
internal inline fun <reified T> RegularFileProperty.bufferReadNullableJsonSet(): Set<T> {
  return orNull?.bufferReadJsonSet() ?: emptySet()
}

internal fun RegularFileProperty.readLines(): List<String> = get().readLines()

internal fun RegularFile.readLines(): List<String> = asFile.readLines()

internal fun RegularFileProperty.readText(): String = get().asFile.readText()

// copied from StringsJVM.kt
internal fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
  if (isNotEmpty()) {
    val firstChar = this[0]
    if (firstChar.isLowerCase()) {
      return buildString {
        val titleChar = firstChar.toTitleCase()
        if (titleChar != firstChar.toUpperCase()) {
          append(titleChar)
        } else {
          append(this@capitalizeSafely.substring(0, 1).toUpperCase(locale))
        }
        append(this@capitalizeSafely.substring(1))
      }
    }
  }
  return this
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
