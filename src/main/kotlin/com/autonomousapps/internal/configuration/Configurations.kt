package com.autonomousapps.internal.configuration

import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Variant
import com.autonomousapps.model.intermediates.Variant.Companion.toVariant

internal object Configurations {
  internal const val CONF_ADVICE_ALL_CONSUMER = "adviceAllConsumer"
  internal const val CONF_ADVICE_ALL_PRODUCER = "adviceAllProducer"

  private val MAIN_SUFFIXES = listOf("api", "implementation", "compileOnly", "runtimeOnly")

  // TODO V2: annotationProcessor is not a prefix, but a suffix!
  private val ANNOTATION_PROCESSOR_PREFIXES = listOf("kapt", "annotationProcessor")

  internal fun isMain(configurationName: String): Boolean {
    return MAIN_SUFFIXES.any { suffix -> configurationName.endsWith(suffix = suffix, ignoreCase = true) }
  }

  internal fun isAnnotationProcessor(configurationName: String): Boolean {
    return ANNOTATION_PROCESSOR_PREFIXES.any { prefix -> configurationName.startsWith(prefix) }
  }

  internal fun isVariant(configurationName: String): Boolean {
    val main = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
    return if (main != null) {
      main != configurationName
    } else {
      ANNOTATION_PROCESSOR_PREFIXES.find { configurationName.startsWith(it) } != configurationName
    }
  }

  internal fun findMain(configurationName: String): String? {
    return MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
      ?: ANNOTATION_PROCESSOR_PREFIXES.find { configurationName.startsWith(it) }
  }

  @OptIn(ExperimentalStdlibApi::class)
  internal fun variantFrom(configurationName: String): Variant {
    val mainBucket = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
    val candidate = if (mainBucket != null) {
      // can be "test...", "testDebug...", "testRelease...", etc.
      val prefix = configurationName.removeSuffix(mainBucket.replaceFirstChar(Char::uppercase))

      if (prefix == "test") {
        // testApi => (main variant, test source set)
        Variant(Variant.VARIANT_NAME_MAIN, SourceSetKind.TEST)
      } else if (prefix.startsWith("test")) {
        prefix.removePrefix("test").replaceFirstChar(Char::lowercase).toVariant(SourceSetKind.TEST)
      } else {
        prefix.toVariant(SourceSetKind.MAIN)
      }
    } else {
      val procBucket = ANNOTATION_PROCESSOR_PREFIXES.find { configurationName.startsWith(it) }
      if (procBucket != null) {
        // can be "kaptTest", "kaptTestDebug", etc.
        val suffix = configurationName.removePrefix(procBucket).replaceFirstChar(Char::lowercase)

        if (suffix == "test") {
          Variant(Variant.VARIANT_NAME_MAIN, SourceSetKind.TEST)
        } else if (suffix.startsWith("test")) {
          suffix.removePrefix("test").replaceFirstChar(Char::lowercase).toVariant(SourceSetKind.TEST)
        } else {
          suffix.toVariant(SourceSetKind.MAIN)
        }
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration $configurationName")
      }
    }

    return if (candidate.variant == configurationName || candidate.variant.isBlank()) Variant.MAIN else candidate
  }
}
