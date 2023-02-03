package com.autonomousapps.model.declaration

import com.autonomousapps.model.declaration.Configurations.Matcher.BY_PREFIX
import com.autonomousapps.model.declaration.Configurations.Matcher.BY_SUFFIX
import com.autonomousapps.model.declaration.Variant.Companion.toVariant
import org.gradle.api.artifacts.Configuration

internal object Configurations {

  internal const val CONF_ADVICE_ALL_CONSUMER = "adviceAllConsumer"
  internal const val CONF_ADVICE_ALL_PRODUCER = "adviceAllProducer"
  internal const val CONF_RESOLVED_DEPS_CONSUMER = "resolvedDepsConsumer"
  internal const val CONF_RESOLVED_DEPS_PRODUCER = "resolvedDepsProducer"

  private val COMPILE_ONLY_SUFFIXES = listOf("compileOnly", "compileOnlyApi", "providedCompile")
  private val MAIN_SUFFIXES = COMPILE_ONLY_SUFFIXES + listOf("api", "implementation", "runtimeOnly")

  private val ANNOTATION_PROCESSOR_TEMPLATES = listOf(
    Template("kapt", BY_PREFIX),
    Template("annotationProcessor", BY_SUFFIX)
  )

  /**
   * "Regular dependency" in contrast to "annotation processor," _not_ in contrast to "test" or other source sets.
   */
  internal fun isForRegularDependency(configurationName: String): Boolean {
    return MAIN_SUFFIXES.any { suffix -> configurationName.endsWith(suffix = suffix, ignoreCase = true) }
  }

  internal fun isForCompileOnly(configurationName: String): Boolean {
    return COMPILE_ONLY_SUFFIXES.any { suffix -> configurationName.endsWith(suffix = suffix, ignoreCase = true) }
  }

  internal fun isForAnnotationProcessor(configurationName: String): Boolean {
    return ANNOTATION_PROCESSOR_TEMPLATES.any { it.matches(configurationName) }
  }

  /**
   * Returns 'null' if the variant to which the configuration belongs is currently unsupported.
   * For example: Test Fixtures or additional Feature Variants.
   */
  @OptIn(ExperimentalStdlibApi::class)
  internal fun variantFrom(configurationName: String, supportedSourceSets: Set<String>, hasCustomSourceSets: Boolean): Variant? {
    val mainBucket = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
    val candidate = if (mainBucket != null) {
      val variantSlug = if (configurationName == mainBucket) {
        // 'main variant' or 'main source set'
        ""
      } else {
        // can be "test...", "testDebug...", "testRelease...", etc.
        configurationName.removeSuffix(mainBucket.replaceFirstChar(Char::uppercase))
      }

      findVariant(variantSlug, supportedSourceSets, hasCustomSourceSets)
    } else {
      val procBucket = ANNOTATION_PROCESSOR_TEMPLATES.find { it.matches(configurationName) }
      if (procBucket != null) {
        val variantSlug = if (configurationName == procBucket.name) {
          // 'main variant' or 'main source set'
          ""
        } else {
          // can be "kaptTest", "kaptTestDebug", "testAnnotationProcessor", etc.
          procBucket.slug(configurationName)
        }

        findVariant(variantSlug, supportedSourceSets, hasCustomSourceSets)
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration $configurationName")
      }
    }

    return if (candidate == null) {
      null
    } else if (candidate.variant == configurationName || candidate.variant.isBlank()) {
      Variant.MAIN
    } else {
      candidate
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun findVariant(variantSlug: String, supportedSourceSets: Set<String>, hasCustomSourceSets: Boolean): Variant? {
    if (variantSlug.isNotEmpty() && !supportedSourceSets.contains(variantSlug)) return null
    return if (variantSlug.isEmpty()) {
      // "" (empty string) always represents the 'main' source set
      variantSlug.toVariant(SourceSetKind.MAIN)
    } else if (variantSlug == Variant.TEST_NAME) {
      // testApi => (main variant, test source set)
      // kaptTest => (main variant, test source set)
      Variant(Variant.MAIN_NAME, SourceSetKind.TEST)
    } else if (hasCustomSourceSets) {
      Variant(variantSlug, SourceSetKind.CUSTOM_JVM)
    } else if (variantSlug.startsWith(Variant.TEST_NAME)) {
      variantSlug.removePrefix(Variant.TEST_NAME)
        .replaceFirstChar(Char::lowercase)
        .toVariant(SourceSetKind.TEST)
    } else if (variantSlug == Variant.ANDROID_TEST_NAME) {
      // androidTestApi => (main variant, androidTest source set)
      // kaptAndroidTest => (main variant, androidTest source set)
      Variant(Variant.MAIN_NAME, SourceSetKind.ANDROID_TEST)
    } else if (variantSlug.startsWith(Variant.ANDROID_TEST_NAME)) {
      variantSlug.removePrefix(Variant.ANDROID_TEST_NAME)
        .replaceFirstChar(Char::lowercase)
        .toVariant(SourceSetKind.ANDROID_TEST)
    } else {
      variantSlug.toVariant(SourceSetKind.MAIN)
    }
  }

  // we want dependency buckets only
  fun Configuration.isForRegularDependency() =
    // do not check '!isCanBeConsumed && !isCanBeResolved' due to https://github.com/gradle/gradle/issues/20547 or
    // similar situations. Other plugins or users (although not recommended) might change these flags. Since we know
    // the exact names of the Configurations we support (based on to which source set they are linked) this check
    // is not necessary.
    isForRegularDependency(name)

  // as in so many things, "kapt" is special: it is a resolvable configuration
  fun Configuration.isForAnnotationProcessor() = isForAnnotationProcessor(name)

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
