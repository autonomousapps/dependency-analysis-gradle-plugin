// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.analyzer.Language
import com.autonomousapps.internal.utils.filterToOrderedSet
import java.io.File

/**
 * Takes a file which is either a jar or a directory that may contain class files and maybe expands it to sibling source
 * sets that also contain class files.
 *
 * E.g., takes `build/classes/java/main` and potentially returns (`build/classes/java/main`, `build/classes/kotlin/main`)
 *
 * Handles all possible source sets, including `main`, `test`, `testFixtures`, etc.
 *
 * Handles all supported languages. See [Language].
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/948#issuecomment-1711177139">Issue 948</a>
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1811">Issue 1811</a>
 */
internal object ArtifactsExpander {
  private const val JAR = "jar"
  private val LANGUAGES = Language.languages()

  internal fun maybeExpand(file: File): Set<File> {
    if (file.extension == JAR) {
      return setOf(file)
    }

    val path = file.invariantSeparatorsPath
    // e.g., main, test, testFixtures
    val sourceSetName = path.substringAfterLast('/')

    // Matches e.g. /foo/bar/producer/build/classes/java/main
    val pathMatcher = Regex(""".+/${Language.languagesPattern()}/$sourceSetName$""")

    return if (path.matches(pathMatcher)) {
      LANGUAGES
        .map { lang -> file.parentFile!!.parentFile!!.resolve(lang).resolve(sourceSetName) }
        .filterToOrderedSet { it.exists() }
    } else {
      setOf(file)
    }
  }
}
