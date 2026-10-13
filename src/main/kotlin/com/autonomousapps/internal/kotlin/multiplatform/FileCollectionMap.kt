// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import com.autonomousapps.internal.providers.isAvailable
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * This type is public because it's a task input, but should be considered an internal implementation detail.
 *
 * File trees can look like this, for example in `commonMain`:
 * - /project/module/src/commonMain/kotlin/Foo.kt
 * - /project/module/src/more/kotlin/Bar.kt
 * - /project/module/build/gen/generated.kt
 *
 * `kotlin` includes source files in:
 * 1. The standard directory (e.g., src/commonMain/kotlin)
 * 2. Any additional directories, configured like `kotlin.sourceSets.commonMain.kotlin.srcDir(project.layout.projectDirectory.dir("src/more/kotlin"))`
 *
 * `generatedKotlin` includes source files in:
 * 1. Any generated source, configured like `kotlin.sourceSets.commonMain.generatedKotlin.srcDir(generatorTask)`
 *
 * `allKotlinSources` (not used here) includes everything:
 * 1. The standard directory (e.g., src/commonMain/kotlin)
 * 2. Any additional directories, configured like `kotlin.sourceSets.commonMain.kotlin.srcDir(project.layout.projectDirectory.dir("src/more/kotlin"))`
 * 3. Any generated source, configured like `kotlin.sourceSets.commonMain.generatedKotlin.srcDir(generatorTask)`
 *
 * @see [KotlinSources]
 */
public data class FileCollectionMap(
  /** E.g., `commonMain`, `commonTest`. */
  val name: String,

  /** The [org.gradle.api.file.FileCollection] for the source set named [name]. Includes generated source. */
  val files: FileCollection,
) {
  internal companion object {
    fun names(kotlinSources: NamedDomainObjectSet<KotlinSourceSet>): List<String> {
      return kotlinSources.map { it.name }
    }

    fun sourceDirectories(kotlinSources: NamedDomainObjectSet<KotlinSourceSet>): List<FileCollection> {
      return kotlinSources.map { it.kotlin.sourceDirectories }
    }

    fun generatedSourceDirectories(kotlinSources: NamedDomainObjectSet<KotlinSourceSet>): List<FileCollection> {
      // nb: `generatedKotlin` requires KGP 2.3+.
      return kotlinSources.map { it.generatedKotlin.sourceDirectories }
    }

    fun sequenced(
      names: Provider<List<String>>,
      files: Provider<List<FileCollection>>,
    ): Sequence<FileCollectionMap> = zipped(names, files).asSequence()

    fun zipped(
      names: Provider<List<String>>,
      files: Provider<List<FileCollection>>,
    ): List<FileCollectionMap> {
      // If all are missing, that's fine, we assume this is an @Optional situation
      if (!names.isPresent && !files.isPresent) {
        return emptyList()
      }
      // If only one is missing, that's an error
      require(names.isPresent && files.isPresent) {
        "Expected 'names' and 'files' to be present. Got names=${names.isAvailable()}, v=${files.isAvailable()}"
      }

      // All are present and must be the same length, or it's an error
      val names = names.get()
      val files = files.get()
      require(names.size == files.size) {
        "Expected 'names.size == files.size'. Got names.size=${names.size}, files.size=${files.size}"
      }

      return names.zip(files) { name, file ->
        FileCollectionMap(name, file)
      }
    }
  }
}
