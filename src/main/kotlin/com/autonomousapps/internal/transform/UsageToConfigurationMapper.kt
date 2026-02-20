// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind

// TODO(tsr): add unit tests?
internal class UsageToConfigurationMapper(
  private val isKaptApplied: Boolean,
  private val projectType: ProjectType,
) {

  /** e.g., "debug" + "implementation" -> "debugImplementation" */
  fun toConfiguration(usage: Usage, forcedKind: SourceKind? = null): String {
    check(usage.bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

    val theSourceKind = forcedKind ?: usage.sourceKind

    if (usage.bucket == Bucket.ANNOTATION_PROCESSOR) {
      val original = processor()
      return if (theSourceKind.name == SourceKind.MAIN_NAME) {
        // "main" + "annotationProcessor" -> "annotationProcessor"
        // "main" + "kapt" -> "kapt"
        when (original) {
          "annotationProcessor" -> "annotationProcessor"
          "kapt" -> "kapt"
          else -> throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      } else {
        // "debug" + "annotationProcessor" -> "debugAnnotationProcessor"
        // "test" + "kapt" -> "kaptTest"
        when (original) {
          "annotationProcessor" -> "${theSourceKind.configurationNamePrefix()}AnnotationProcessor"
          "kapt" -> "kapt${theSourceKind.configurationNameSuffix()}"
          else -> throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      }
    }

    // not an annotation processor
    return if (theSourceKind.name == SourceKind.MAIN_NAME && theSourceKind.kind == SourceKind.MAIN_KIND) {
      // "main" + "api" -> "api"
      usage.bucket.value
    } else {
      // "debug" + "implementation" -> "debugImplementation"
      // "test" + "implementation" -> "testImplementation"
      "${theSourceKind.configurationNamePrefix()}${usage.bucket.value.replaceFirstChar(Char::uppercase)}"
    }
  }

  private fun processor() = if (isKaptApplied) "kapt" else "annotationProcessor"

  private fun SourceKind.configurationNamePrefix(): String {
    return when (kind) {
      SourceKind.CUSTOM_JVM_KIND -> name
      SourceKind.MAIN_KIND -> name
      SourceKind.TEST_KIND -> variantName(SourceKind.TEST_NAME)
      SourceKind.ANDROID_TEST_FIXTURES_KIND -> variantName(SourceKind.TEST_FIXTURES_NAME)
      SourceKind.ANDROID_TEST_KIND -> variantName(SourceKind.ANDROID_TEST_NAME)
      else -> error("Unexpected kind: $kind")
    }
  }

  // TODO(tsr): this needs similar handling to above.
  private fun SourceKind.configurationNameSuffix(): String = when (kind) {
    SourceKind.MAIN_KIND -> name.replaceFirstChar(Char::uppercase)
    SourceKind.TEST_KIND -> "Test"
    SourceKind.ANDROID_TEST_FIXTURES_KIND -> "TestFixtures"
    SourceKind.ANDROID_TEST_KIND -> "AndroidTest"
    SourceKind.CUSTOM_JVM_KIND -> name.replaceFirstChar(Char::uppercase)
    else -> error("Unexpected kind: $kind")
  }

  private fun SourceKind.variantName(sourceKindName: String): String {
    return if (projectType == ProjectType.ANDROID && name != sourceKindName) {
      "$name${sourceKindName.replaceFirstChar(Char::uppercase)}"
    } else {
      sourceKindName
    }
  }
}
