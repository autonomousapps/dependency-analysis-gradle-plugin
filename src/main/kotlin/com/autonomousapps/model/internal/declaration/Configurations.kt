// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.model.internal.declaration.Configurations.Matcher.BY_PREFIX
import com.autonomousapps.model.internal.declaration.Configurations.Matcher.BY_SUFFIX
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.SourceKind
import org.gradle.api.artifacts.Configuration

internal object Configurations {

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
   * Infers a [SourceKind] from a [configurationName]. Wil return null if the sourceKind to which the configuration
   * belongs is currently unsupported.
   */
  internal fun sourceKindFrom(
    configurationName: String,
    supportedSourceSets: Set<String>,
    isAndroidProject: Boolean,
    hasCustomSourceSets: Boolean,
  ): SourceKind? {
    val mainBucket = MAIN_SUFFIXES.find { configurationName.endsWith(suffix = it, ignoreCase = true) }

    val candidate = if (mainBucket != null) {
      val variantSlug = if (configurationName == mainBucket) {
        // 'main variant' or 'main source set'
        ""
      } else {
        // can be "test...", "testDebug...", "testRelease...", etc.
        configurationName.removeSuffix(mainBucket.replaceFirstChar(Char::uppercase))
      }

      findSourceKind(
        variantSlug,
        supportedSourceSets,
        isAndroidProject = isAndroidProject,
        hasCustomSourceSets = hasCustomSourceSets
      )
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

        findSourceKind(
          variantSlug,
          supportedSourceSets,
          isAndroidProject = isAndroidProject,
          hasCustomSourceSets = hasCustomSourceSets
        )
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration '$configurationName'")
      }
    }

    return if (candidate == null) {
      null
    } else if (candidate.name == configurationName || candidate.name.isBlank()) {
      if (isAndroidProject) {
        AndroidSourceKind.MAIN
      } else {
        JvmSourceKind.MAIN
      }
    } else {
      candidate
    }
  }

  private fun findSourceKind(
    variantSlug: String,
    supportedSourceSets: Set<String>,
    isAndroidProject: Boolean,
    hasCustomSourceSets: Boolean,
  ): SourceKind? {
    if (variantSlug.isNotEmpty() && !supportedSourceSets.contains(variantSlug)) {
      return null
    }

    return if (variantSlug.isEmpty()) {
      // "" (empty string) always represents the 'main' source set
      if (isAndroidProject) {
        AndroidSourceKind.MAIN
      } else {
        JvmSourceKind.MAIN
      }
    } else if (variantSlug == SourceKind.TEST_NAME) {
      // testApi => (main variant, test source set)
      // kaptTest => (main variant, test source set)
      if (isAndroidProject) {
        AndroidSourceKind.TEST
      } else {
        JvmSourceKind.TEST
      }
    } else if (variantSlug == SourceKind.ANDROID_TEST_NAME) {
      // must be Android
      require(isAndroidProject) { "Expected Android project" }
      // androidTestApi => (main variant, androidTest source set)
      // kaptAndroidTest => (main variant, androidTest source set)
      AndroidSourceKind.ANDROID_TEST
    } else if (hasCustomSourceSets) {
      // can't be Android
      require(!isAndroidProject) { "Expected JVM project" }
      JvmSourceKind.of(variantSlug)
    } else if (variantSlug == SourceKind.TEST_FIXTURES_NAME) {
      require(isAndroidProject) { "Expected Android project" }
      AndroidSourceKind.ANDROID_TEST_FIXTURES
    } else if (variantSlug.startsWith(SourceKind.TEST_FIXTURES_NAME)) {
      require(isAndroidProject) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.TEST_FIXTURES_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.testFixtures(name)
    } else if (variantSlug.startsWith(SourceKind.TEST_NAME)) {
      // must be Android
      require(isAndroidProject) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.TEST_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.test(name)
    } else if (variantSlug.startsWith(SourceKind.ANDROID_TEST_NAME)) {
      // must be Android
      require(isAndroidProject) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.ANDROID_TEST_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.androidTest(name)
    } else {
      // TODO(tsr): when do we hit this case?
      if (isAndroidProject) {
        AndroidSourceKind.main(variantSlug)
      } else {
        JvmSourceKind.of(variantSlug)
      }
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
