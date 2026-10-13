// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.concurrent.Callable

/**
 * Provides lazy access to Kotlin sources, including generated sources, as directory hierarchies.
 *
 * @see [FileCollectionMap]
 */
internal class KotlinSources private constructor(
  private val kotlinSourceSets: NamedDomainObjectSet<KotlinSourceSet>,
) {

  fun names(): Callable<List<String>> = Callable { FileCollectionMap.names(kotlinSourceSets) }
  fun sourceDirectories(): Callable<List<FileCollection>> =
    Callable { FileCollectionMap.sourceDirectories(kotlinSourceSets) }

  fun generatedSourceDirectories(): Callable<List<FileCollection>> =
    Callable { FileCollectionMap.generatedSourceDirectories(kotlinSourceSets) }

  internal companion object {
    /** Provides lazy access to Kotlin sources in `commonMain` and `commonTest`, including generated sources. */
    fun all(kotlin: KotlinMultiplatformExtension): KotlinSources = sources(kotlin)

    /** Provides lazy access to Kotlin sources in `commonMain`, including generated sources. */
    fun commonMain(kotlin: KotlinMultiplatformExtension): KotlinSources {
      return sources(kotlin) { sourceSet -> sourceSet.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME }
    }

    /** Provides lazy access to Kotlin sources in `commonTest`, including generated sources. */
    fun commonTest(kotlin: KotlinMultiplatformExtension): KotlinSources {
      return sources(kotlin) { sourceSet -> sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME }
    }

    private fun sources(kotlin: KotlinMultiplatformExtension): KotlinSources {
      // the benefit of this form is that `matching` eagerly realizes source sets
      return KotlinSources(kotlin.sourceSets)
    }

    private fun sources(
      kotlin: KotlinMultiplatformExtension,
      predicate: (KotlinSourceSet) -> Boolean,
    ): KotlinSources {
      // nb: `matching` eagerly realizes collection elements
      return KotlinSources(kotlin.sourceSets.matching(predicate))
    }
  }
}
