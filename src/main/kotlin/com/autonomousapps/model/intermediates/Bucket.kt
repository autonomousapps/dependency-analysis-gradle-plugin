package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.configuration.Configurations

/** Standard user-facing dependency buckets, variant-agnostic. */
internal enum class Bucket(val value: String) {
  API("api"),
  IMPL("implementation"),
  COMPILE_ONLY("compileOnly"),
  RUNTIME_ONLY("runtimeOnly"),

  // note that only the java-library plugin currently supports this configuration
  // COMPILE_ONLY_API("compileOnlyApi"),

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
    fun of(configurationName: String): Bucket {
      if (Configurations.isForAnnotationProcessor(configurationName)) return ANNOTATION_PROCESSOR

      return values().find { bucket ->
        configurationName.endsWith(bucket.value, true)
      } ?: throw IllegalArgumentException("No matching bucket for $configurationName")
    }

    val VISIBLE_DOWNSTREAM = listOf(API, IMPL, ANNOTATION_PROCESSOR)
  }
}
