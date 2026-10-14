// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.concurrent.Callable

/** Provides lazy access to Kotlin sources in `commonMain` and `commonTest`, including generated sources. */
internal class KotlinCommonSources private constructor(
  private val kotlinSources: NamedDomainObjectSet<KotlinSourceSet>,
) {

  internal companion object {
    private val commonSources = setOf(
      KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME,
      KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME,
    )

    /** Provides lazy access to Kotlin sources in `commonMain` and `commonTest`, including generated sources. */
    fun all(kotlin: KotlinMultiplatformExtension): KotlinCommonSources {
      return sources(kotlin) { sourceSet -> sourceSet.name in commonSources }
    }

    /** Provides lazy access to Kotlin sources in `commonMain`, including generated sources. */
    fun commonMain(kotlin: KotlinMultiplatformExtension): KotlinCommonSources {
      return sources(kotlin) { sourceSet -> sourceSet.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME }
    }

    /** Provides lazy access to Kotlin sources in `commonTest`, including generated sources. */
    fun commonTest(kotlin: KotlinMultiplatformExtension): KotlinCommonSources {
      return sources(kotlin) { sourceSet -> sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME }
    }

    private fun sources(
      kotlin: KotlinMultiplatformExtension,
      predicate: (KotlinSourceSet) -> Boolean,
    ): KotlinCommonSources {
      val sources = kotlin.sourceSets.matching(predicate)
      return KotlinCommonSources(sources)
    }
  }

  fun names(): Callable<List<String>> = Callable { FileCollectionMap.names(kotlinSources) }
  fun files(): Callable<List<FileCollection>> = Callable { FileCollectionMap.files(kotlinSources) }
}
