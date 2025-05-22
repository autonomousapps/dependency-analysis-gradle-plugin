// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.declaration.internal

import com.autonomousapps.internal.utils.reallyAll
import com.autonomousapps.model.declaration.internal.Bucket.Companion.VISIBLE_TO_TEST_COMPILE
import com.autonomousapps.model.declaration.internal.Bucket.Companion.VISIBLE_TO_TEST_RUNTIME
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
     * [SourceKind.MAIN][com.autonomousapps.model.source.SourceKind.MAIN_KIND] to
     * [SourceKind.TEST][com.autonomousapps.model.source.SourceKind.TEST_KIND] and
     * [SourceKind.ANDROID_TEST][com.autonomousapps.model.source.SourceKind.ANDROID_TEST_KIND] compile classpaths. This
     * is necessary for correct advice relating to test source.
     *
     * @see [VISIBLE_TO_TEST_RUNTIME]
     */
    private val VISIBLE_TO_TEST_COMPILE = listOf(API, IMPL)

    /**
     * [Declarations][Declaration] in these buckets are visible from
     * [SourceKind.MAIN][com.autonomousapps.model.source.SourceKind.MAIN_KIND] to
     * [SourceKind.TEST][com.autonomousapps.model.source.SourceKind.TEST_KIND] and
     * [SourceKind.ANDROID_TEST][com.autonomousapps.model.source.SourceKind.ANDROID_TEST_KIND] runtime classpaths. This
     * is necessary for correct advice relating to test source.
     *
     * @see [VISIBLE_TO_TEST_COMPILE]
     */
    private val VISIBLE_TO_TEST_RUNTIME = listOf(API, IMPL, RUNTIME_ONLY)

    fun determineVisibility(usages: Set<Usage>, declarations: Set<Declaration>): Visibility {
      val compileVisibility = isVisibleToTestCompileClasspath(usages, declarations)
      val runtimeVisibility = isVisibleToTestRuntimeClasspath(usages, declarations)

      return Visibility(forCompile = compileVisibility, forRuntime = runtimeVisibility)
    }

    /**
     * A dependency is visible from main to test source iff it is in the correct bucket ([VISIBLE_TO_TEST_COMPILE])
     * _and_ if it is declared on any of the [VISIBLE_TO_TEST_COMPILE] configurations.
     *
     * Note that the `compileOnly` configuration _is not_ visible to the `testImplementation` configuration.
     *
     * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#resolvable_configurations.">Java configurations</a>
     */
    fun isVisibleToTestCompileClasspath(usages: Set<Usage>, declarations: Set<Declaration>): Boolean {
      return isVisibleIn(VISIBLE_TO_TEST_COMPILE, usages, declarations)
    }

    /**
     * A dependency is visible from main to test source iff it is in the correct bucket ([VISIBLE_TO_TEST_RUNTIME])
     * _and_ if it is declared on any of the [VISIBLE_TO_TEST_RUNTIME] configurations.
     *
     * Note that the `compileOnly` configuration _is not_ visible to the `testImplementation` configuration.
     *
     * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#resolvable_configurations.">Java configurations</a>
     */
    fun isVisibleToTestRuntimeClasspath(usages: Set<Usage>, declarations: Set<Declaration>): Boolean {
      return isVisibleIn(VISIBLE_TO_TEST_RUNTIME, usages, declarations)
    }

    private fun isVisibleIn(buckets: List<Bucket>, usages: Set<Usage>, declarations: Set<Declaration>): Boolean {
      return usages.reallyAll { usage ->
        buckets.any { it == usage.bucket }
          && declarations.any { declaration -> buckets.any { it.matches(declaration) } }
      }
    }
  }

  class Visibility(val forCompile: Boolean, val forRuntime: Boolean)
}
