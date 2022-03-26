package com.autonomousapps.internal.configuration

import com.autonomousapps.internal.configuration.Configurations.Matcher.BY_PREFIX
import com.autonomousapps.internal.configuration.Configurations.Matcher.BY_SUFFIX
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Variant
import com.autonomousapps.model.intermediates.Variant.Companion.toVariant

internal object Configurations {

  internal const val CONF_ADVICE_ALL_CONSUMER = "adviceAllConsumer"
  internal const val CONF_ADVICE_ALL_PRODUCER = "adviceAllProducer"

  private val MAIN_SUFFIXES = listOf("api", "implementation", "compileOnly", "runtimeOnly")

  private val ANNOTATION_PROCESSOR_PREFIXES = listOf(
    Template("kapt", BY_PREFIX),
    Template("annotationProcessor", BY_SUFFIX)
  )

  /**
   * Poorly named. "Main" in contrast to "annotation processor," _not_ in contrast to "test" or other source sets.
   */
  internal fun isMain(configurationName: String): Boolean {
    return MAIN_SUFFIXES.any { suffix -> configurationName.endsWith(suffix = suffix, ignoreCase = true) }
  }

  internal fun isAnnotationProcessor(configurationName: String): Boolean {
    return ANNOTATION_PROCESSOR_PREFIXES.any { it.matches(configurationName) }
  }

  internal fun isVariant(configurationName: String): Boolean {
    val main = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
    return if (main != null) {
      main != configurationName
    } else {
      ANNOTATION_PROCESSOR_PREFIXES.find { it.matches(configurationName) }?.name != configurationName
    }
  }

  // TODO this code is buggy in the presence of unknown configuration names. E.g., "androidTestImplementation" maps to the
  //  nonsensical variant `Variant(androidTest, MAIN)`.
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
      val procBucket = ANNOTATION_PROCESSOR_PREFIXES.find { it.matches(configurationName) }
      if (procBucket != null) {
        // can be "kaptTest", "kaptTestDebug", "testAnnotationProcessor", etc.
        val variantSlug = procBucket.slug(configurationName)

        if (variantSlug == "test") {
          Variant(Variant.VARIANT_NAME_MAIN, SourceSetKind.TEST)
        } else if (variantSlug.startsWith("test")) {
          variantSlug.removePrefix("test").replaceFirstChar(Char::lowercase).toVariant(SourceSetKind.TEST)
        } else {
          variantSlug.toVariant(SourceSetKind.MAIN)
        }
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration $configurationName")
      }
    }

    return if (candidate.variant == configurationName || candidate.variant.isBlank()) Variant.MAIN else candidate
  }

  private class Template(
    val name: String,
    val matcher: Matcher
  ) {
    fun matches(other: String): Boolean = matcher.matches(name, other)
    fun slug(other: String): String = matcher.slug(name, other)
  }

  private enum class Matcher {
    BY_PREFIX,
    BY_SUFFIX;

    fun matches(template: String, concreteValue: String): Boolean = when (this) {
      BY_PREFIX -> concreteValue.startsWith(template)
      BY_SUFFIX -> concreteValue.endsWith(template, ignoreCase = true)
    }

    // BY_PREFIX: "kaptTest" -> "test"
    // BY_SUFFIX: "testAnnotationProcessor"" -> "test"
    @OptIn(ExperimentalStdlibApi::class)
    fun slug(template: String, concreteValue: String): String = when (this) {
      BY_PREFIX -> concreteValue.removePrefix(template).replaceFirstChar(Char::lowercase)
      BY_SUFFIX -> concreteValue.removeSuffix(template.replaceFirstChar(Char::uppercase))
    }
  }
}
