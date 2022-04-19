package com.autonomousapps.model.declaration

/** Standard user-facing dependency buckets (such as **implementation** and **api**), [variant][Variant]-agnostic. */
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
    @JvmStatic
    fun of(configurationName: String): Bucket {
      if (Configurations.isForAnnotationProcessor(configurationName)) return ANNOTATION_PROCESSOR

      return values().find { bucket ->
        configurationName.endsWith(bucket.value, true)
      } ?: throw IllegalArgumentException("No matching bucket for $configurationName")
    }

    /**
     * [Declarations][Declaration] in these buckets are visible from [SourceSetKind.MAIN] to [SourceSetKind.TEST]. This
     * is necessary for correct advice relating to test source.
     */
    val VISIBLE_TO_TEST_SOURCE = listOf(API, IMPL, ANNOTATION_PROCESSOR)
  }
}
