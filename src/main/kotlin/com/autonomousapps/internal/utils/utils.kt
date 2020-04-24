@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import java.io.File
import java.util.*

/**
 * Resolves the file from the property and deletes its constants, then returns the file.
 */
internal fun RegularFileProperty.getAndDelete(): File {
  val file = get().asFile
  file.delete()
  return file
}

internal inline fun <reified T> RegularFileProperty.fromJsonSet(): Set<T> {
  return get().asFile.readText().fromJsonSet()
}

internal inline fun <reified T> RegularFileProperty.fromJson(): T {
  return get().asFile.readText().fromJson()
}

internal inline fun <reified T> RegularFileProperty.fromNullableJsonSet(): Set<T>? {
  return orNull?.asFile?.readText()?.fromJsonSet()
}

internal fun RegularFileProperty.readLines(): List<String> {
  return get().asFile.readLines()
}

// Print dependency tree (like running the `dependencies` task).
@Suppress("unused")
internal fun printDependencyTree(dependencies: Set<DependencyResult>, level: Int = 0) {
  dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { result ->
    val resolvedComponentResult = result.selected
    println("${"  ".repeat(level)}- ${resolvedComponentResult.id}")
    printDependencyTree(resolvedComponentResult.dependencies, level + 1)
  }
}

// copied from StringsJVM.kt
fun String.capitalizeSafely(locale: Locale = Locale.ROOT): String {
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
