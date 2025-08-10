// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.internal.utils.reallyAll
import com.autonomousapps.model.internal.declaration.Bucket.Companion.VISIBLE_TO_TEST_SOURCE
import com.autonomousapps.model.internal.intermediates.Usage
import com.squareup.moshi.JsonClass

/**
 * Standard user-facing dependency buckets (such as **implementation** and **api**),
 * [variant][com.autonomousapps.model.source.SourceKind]-agnostic.
 */
@JsonClass(generateAdapter = false)
internal enum class Bucket(val value: String) {
  API("api"),
  IMPL("implementation"),

  // These configurations go into the compileOnly bucket: '...CompileOny', '...CompileOnlyApi', 'providedCompile'
  COMPILE_ONLY("compileOnly"),
  RUNTIME_ONLY("runtimeOnly"),

  // TODO: somewhat problematic since this value can be used naively. Should probably be a function that can return
  //  either kapt or annotationProcessor...
  ANNOTATION_PROCESSOR("annotationProcessor"),

  /** Unused. */
  NONE("n/a"),
  ;

  fun matches(declaration: Declaration): Boolean {
    return this == declaration.bucket
  }

  companion object {
    @JvmStatic
    fun of(configurationName: String): Bucket {
      if (Configurations.isForAnnotationProcessor(configurationName)) return ANNOTATION_PROCESSOR
      if (Configurations.isForCompileOnly(configurationName)) return COMPILE_ONLY

      return values().find { bucket ->
        configurationName.endsWith(bucket.value, true)
      } ?: throw IllegalArgumentException("No matching bucket for $configurationName")
    }

    /**
     * [Declarations][Declaration] in these buckets are visible from
     * [SourceKind.MAIN][com.autonomousapps.model.source.SourceKind.MAIN] to
     * [SourceKind.TEST][com.autonomousapps.model.source.SourceKind.TEST] and
     * [SourceKind.ANDROID_TEST][com.autonomousapps.model.source.SourceKind.ANDROID_TEST]. This is necessary
     * for correct advice relating to test source.
     *
     * TODO(tsr): wait, is this actually true for ANNOTATION_PROCESSOR as well? That seems like an error. Oh, maybe it
     *  was true for an older version of Gradle?
     */
    private val VISIBLE_TO_TEST_SOURCE = listOf(API, IMPL, ANNOTATION_PROCESSOR)

    /**
     * A dependency is visible from main to test source iff it is in the correct bucket ([VISIBLE_TO_TEST_SOURCE]) _and_
     * if it is declared on either the [API] or [IMPL] configurations.
     *
     * Note that the `compileOnly` configuration _is not_ visible to the `testImplementation` configuration.
     *
     * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#resolvable_configurations.">Java configurations</a>
     */
    fun isVisibleToTestSource(usages: Set<Usage>, declarations: Set<Declaration>): Boolean {
      return usages.reallyAll { usage ->
        VISIBLE_TO_TEST_SOURCE.any { it == usage.bucket }
          && declarations.any { API.matches(it) || IMPL.matches(it) }
      }
    }
  }
}
