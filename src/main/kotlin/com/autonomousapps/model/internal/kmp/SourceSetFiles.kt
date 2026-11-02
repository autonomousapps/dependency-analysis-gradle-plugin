// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.ClassAnalyzer
import com.autonomousapps.internal.kotlin.ClassBinarySignature
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.capitalizeSafely
import com.squareup.moshi.JsonClass

/** Only public because it's a task input. */
@JsonClass(generateAdapter = false)
public data class SourceSetFiles(
  /** e.g., "commonMain", "commonTest". */
  val sourceSetName: String,
  /** e.g., "src/commonMain/kotlin", "build/gen/kotlin". */
  val sourceBase: String,
  /** e.g., "com/foo/Foo.kt", "generated.kt". */
  val sourceFiles: Set<String>,
) : Comparable<SourceSetFiles> {

  init {
    assert(sourceFiles.isNotEmpty()) { "Expected non-empty 'sourceFiles'." }
    assert(sourceBase.isNotBlank()) { "Expected non-blank 'sourceBase'." }
  }

  override fun compareTo(other: SourceSetFiles): Int {
    return compareBy(SourceSetFiles::sourceSetName)
      .thenBy(SourceSetFiles::sourceBase)
      .thenBy(LexicographicIterableComparator()) { it.sourceFiles }
      .compare(this, other)
  }

  internal companion object {
    /**
     * For KMP projects, we associate the analyzed class file with the source file on disk, to bridge the compilation-
     * source set gap. This value should always be non-null for KMP projects, and null otherwise.
     */
    fun Set<SourceSetFiles>.findSourceSetName(classAnalyzer: ClassAnalyzer): String? {
      return this
        .find { sources ->
          sources.sourceFiles.any { f ->
            // a standard source/class file
            f.removeSuffix(".kt") == classAnalyzer.className
              // a class created from top-level functions in the root package
              || f == classAnalyzer.source
              // a class created from top-level functions in a non-root package
              || f == classAnalyzer.className.substringBeforeLast('/') + '/' + classAnalyzer.source
          }
        }
        ?.sourceSetName
    }

    fun Set<SourceSetFiles>.findSourceSetName(classSignature: ClassBinarySignature): String? {
      return this
        .find { sources ->
          sources.sourceFiles.any { f ->
            // a standard source/class file
            f.removeSuffix(".kt") == classSignature.name
              // a class created from top-level functions
              || f.asClassSignature() == classSignature.name
          }
        }
        ?.sourceSetName
    }

    /**
     * For files that only contain top-level members.
     *
     * a/b/generated.kt => a/b/GeneratedKt
     */
    private fun String.asClassSignature(): String {
      val i = 1 + indexOfLast { it == '/' }
      if (i == 0) {
        return capitalizeSafely().removeSuffix(".kt") + "Kt"
      }

      val pkg = substring(0, i)
      // `length - 3` removes the `".kt"` suffix
      val className = substring(i, length - 3).capitalizeSafely() + "Kt"
      return "$pkg$className"
    }

    /**
     * For KMP projects, we must find the source set name associated with [relativePath] so we can produce precise
     * analysis.
     */
    fun Set<SourceSetFiles>.findSourceSetName(relativePath: String): String {
      return this
        // We use `first` because there must be a match
        .first { sources ->
          // "src/commonMain/kotlin" + "/" + "com/foo/Bar.kt"
          sources.sourceFiles.any { f -> "${sources.sourceBase}/$f" == relativePath }
        }
        .sourceSetName
    }
  }
}
