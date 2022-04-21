package com.autonomousapps.model.declaration

import com.autonomousapps.model.declaration.Configurations.Matcher.BY_PREFIX
import com.autonomousapps.model.declaration.Configurations.Matcher.BY_SUFFIX
import com.autonomousapps.model.declaration.Variant.Companion.toVariant
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet

internal object Configurations {

  internal const val CONF_ADVICE_ALL_CONSUMER = "adviceAllConsumer"
  internal const val CONF_ADVICE_ALL_PRODUCER = "adviceAllProducer"

  private val MAIN_SUFFIXES = listOf("api", "implementation", "compileOnly", "runtimeOnly")

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

  internal fun isForAnnotationProcessor(configurationName: String): Boolean {
    return ANNOTATION_PROCESSOR_TEMPLATES.any { it.matches(configurationName) }
  }

  internal fun isVariant(configurationName: String): Boolean {
    val main = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }
    return if (main != null) {
      main != configurationName
    } else {
      ANNOTATION_PROCESSOR_TEMPLATES.find { it.matches(configurationName) }?.name != configurationName
    }
  }

  internal fun splitPrefix(prefix:String): Pair<String, String> {
    if (MAIN_SUFFIXES.contains(prefix)) {
      return Pair(SourceSet.MAIN_SOURCE_SET_NAME, Variant.BASE_VARIANT)
    }

    // TODO use more context (e.g. which source sets exist) to do this correctly
    val sourceSetName = prefix
    val androidVariant = Variant.BASE_VARIANT
    return Pair(sourceSetName, androidVariant)
  }

  @OptIn(ExperimentalStdlibApi::class)
  internal fun variantFrom(configurationName: String): Variant {
    val mainBucket = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }

    val candidate = if (mainBucket != null) {
      // can be "test...", "testDebug...", "testRelease...", etc.
      val prefix = configurationName.removeSuffix(mainBucket.replaceFirstChar(Char::uppercase))
      // testApi => (base variant, test source set)
      val (sourceSetName, androidVariant) = splitPrefix(prefix)
      androidVariant.toVariant(sourceSetName)
    } else {
      val procBucket = ANNOTATION_PROCESSOR_TEMPLATES.find { it.matches(configurationName) }
      if (procBucket != null) {
        // can be "kaptTest", "kaptTestDebug", "testAnnotationProcessor", etc.
        val variantSlug = procBucket.slug(configurationName)
        val (sourceSetName, androidVariant) = splitPrefix(variantSlug)
        androidVariant.toVariant(sourceSetName)
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration $configurationName")
      }
    }

    return candidate
  }

  // we want dependency buckets only
  fun Configuration.isForRegularDependency() =
    !isCanBeConsumed && !isCanBeResolved && isForRegularDependency(name)

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
