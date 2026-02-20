// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind

/** Utility for mapping configuration names to related types. */
internal class ConfigurationNames(
  val projectType: ProjectType,
  private val supportedSourceSetNames: Set<String>,
) {

  private companion object {
    val COMPILE_ONLY = listOf("compileOnly")
    val COMPILE_ONLY_SUFFIXES_ANDROID = COMPILE_ONLY
    val COMPILE_ONLY_SUFFIXES_KMP = COMPILE_ONLY

    // I'm pretty sure KMP doesn't support `compileOnlyApi`, and `providedCompile` is ancient (deprecated? gone?)
    val COMPILE_ONLY_SUFFIXES_JVM = COMPILE_ONLY + listOf("compileOnlyApi", "providedCompile")
    val RUNTIME_SUFFIXES = listOf("api", "implementation", "runtimeOnly")

    val MAIN_SUFFIXES_ANDROID = COMPILE_ONLY_SUFFIXES_ANDROID + RUNTIME_SUFFIXES
    val MAIN_SUFFIXES_KMP = COMPILE_ONLY_SUFFIXES_KMP + RUNTIME_SUFFIXES
    val MAIN_SUFFIXES_JVM = COMPILE_ONLY_SUFFIXES_JVM + RUNTIME_SUFFIXES

    fun getCompileOnlySuffixes(projectType: ProjectType): List<String> {
      return when (projectType) {
        ProjectType.ANDROID -> COMPILE_ONLY_SUFFIXES_ANDROID
        ProjectType.JVM -> COMPILE_ONLY_SUFFIXES_JVM
        ProjectType.KMP -> COMPILE_ONLY_SUFFIXES_KMP
      }
    }

    fun getMainSuffixes(projectType: ProjectType): List<String> {
      return when (projectType) {
        ProjectType.ANDROID -> MAIN_SUFFIXES_ANDROID
        ProjectType.JVM -> MAIN_SUFFIXES_JVM
        ProjectType.KMP -> MAIN_SUFFIXES_KMP
      }
    }
  }

  private val annotationProcessorAnnotationProcessorNameMatchers = listOf(
    AnnotationProcessorNameMatcher(
      AnnotationProcessorNameMatcher.AnnotationProcessor.KAPT,
      projectType,
      supportedSourceSetNames
    ),
    AnnotationProcessorNameMatcher(
      AnnotationProcessorNameMatcher.AnnotationProcessor.ANNOTATION_PROCESSOR,
      projectType,
      supportedSourceSetNames
    ),
  )

  private val expectedCompileOnlyConfigurationNames: Set<String> by unsafeLazy {
    supportedSourceSetNames.flatMapToOrderedSet { sourceSetName ->
      getCompileOnlySuffixes(projectType).map { suffix ->
        mainAwareConcat(sourceSetName, suffix)
      }
    }
  }

  private val expectedMainConfigurationNames: Set<String> by unsafeLazy {
    supportedSourceSetNames.flatMapToOrderedSet { sourceSetName ->
      getMainSuffixes(projectType).map { suffix ->
        mainAwareConcat(sourceSetName, suffix)
      }
    }
  }

  private fun mainAwareConcat(sourceSetName: String, bucket: String): String {
    return if (sourceSetName == "main") {
      // main + compileOnly => compileOnly
      bucket
    } else {
      // test + compileOnly => testCompileOnly
      sourceSetName + bucket.replaceFirstChar(Char::uppercase)
    }
  }

  /**
   * "Regular dependency" in contrast to "annotation processor," _not_ in contrast to "test" or other source sets.
   *
   * Android examples include:
   * * `api`
   * * `implementation`
   * * `compileOnly`
   * * `runtimeOnly`
   * * `testImplementation`
   * * `debugImplementation`
   * * `flavorReleaseApi`
   * * etc.
   *
   * JVM examples include:
   * * `api`
   * * `implementation`
   * * `compileOnly`
   * * `runtimeOnly`
   * * `testImplementation`
   * * etc.
   *
   * KMP examples include:
   * * `commonMainApi`
   * * `commonMainImplementation`
   * * `iosArm64MainCompileOnly`
   * * `jvmTestRuntimeOnly`
   * * etc.
   *
   * Do not check `!isCanBeConsumed && !isCanBeResolved` due to https://github.com/gradle/gradle/issues/20547 or
   * similar situations. Other plugins or users (although not recommended) might change these flags. Since we know
   * the exact names of the Configurations we support (based on to which source set they are linked) this check is not
   * necessary.
   */
  fun isForRegularDependency(configurationName: String): Boolean = configurationName in expectedMainConfigurationNames

  fun isForCompileOnly(configurationName: String): Boolean = configurationName in expectedCompileOnlyConfigurationNames

  fun isForAnnotationProcessor(configurationName: String): Boolean {
    return annotationProcessorAnnotationProcessorNameMatchers.any { it.matches(configurationName) }
  }

  fun isDependencyBucket(configurationName: String): Boolean {
    return isForRegularDependency(configurationName) || isForAnnotationProcessor(configurationName)
  }

  /**
   * Infers a [SourceKind] from a [configurationName]. Will return null if the `sourceKind` to which the configuration
   * belongs is not in [supportedSourceSetNames].
   */
  internal fun sourceKindFrom(
    configurationName: String,
    hasCustomSourceSets: Boolean,
  ): SourceKind? {
    val mainBucket = getMainSuffixes(projectType).find { configurationName.endsWith(suffix = it, ignoreCase = true) }

    val candidate = if (mainBucket != null) {
      val variantSlug = if (configurationName == mainBucket) {
        // 'main variant' or 'main source set'
        ""
      } else {
        // can be "test...", "testDebug...", "testRelease...", etc.
        configurationName.removeSuffix(mainBucket.replaceFirstChar(Char::uppercase))
      }

      findSourceKind(
        variantSlug = variantSlug,
        hasCustomSourceSets = hasCustomSourceSets,
      )
    } else {
      val procBucket = annotationProcessorAnnotationProcessorNameMatchers.find { it.matches(configurationName) }
      if (procBucket != null) {
        val variantSlug = if (configurationName == procBucket.kind.value) {
          // 'main variant' or 'main source set'
          ""
        } else {
          // can be "kaptTest", "kaptTestDebug", "testAnnotationProcessor", etc.
          procBucket.slug(configurationName)
        }

        findSourceKind(
          variantSlug = variantSlug,
          hasCustomSourceSets = hasCustomSourceSets,
        )
      } else {
        throw IllegalArgumentException("Cannot find variant for configuration '$configurationName'")
      }
    }

    return if (candidate == null) {
      null
    } else if (candidate.name == configurationName || candidate.name.isBlank()) {
      when (projectType) {
        ProjectType.ANDROID -> AndroidSourceKind.MAIN
        ProjectType.JVM -> JvmSourceKind.MAIN
        ProjectType.KMP -> KmpSourceKind.JVM_MAIN // TODO(projectType): what about when we have non-JVM targets?
      }
    } else {
      candidate
    }
  }

  private fun findSourceKind(
    variantSlug: String,
    hasCustomSourceSets: Boolean,
  ): SourceKind? {
    if (variantSlug.isNotEmpty() && !supportedSourceSetNames.contains(variantSlug)) {
      return null
    }

    return if (variantSlug.isEmpty()) {
      // "" (empty string) always represents the 'main' source set
      when (projectType) {
        ProjectType.ANDROID -> AndroidSourceKind.MAIN
        ProjectType.JVM -> JvmSourceKind.MAIN
        ProjectType.KMP -> KmpSourceKind.JVM_MAIN
      }
    } else if (variantSlug == SourceKind.TEST_NAME) {
      require(projectType != ProjectType.KMP) { "Expected non-KMP project" }
      // testApi => (main variant, test source set)
      // kaptTest => (main variant, test source set)
      if (projectType == ProjectType.ANDROID) {
        AndroidSourceKind.TEST
      } else {
        JvmSourceKind.TEST
      }
    } else if (variantSlug == SourceKind.ANDROID_TEST_NAME) {
      // must be Android
      require(projectType == ProjectType.ANDROID) { "Expected Android project" }
      // androidTestApi => (main variant, androidTest source set)
      // kaptAndroidTest => (main variant, androidTest source set)
      AndroidSourceKind.ANDROID_TEST
    } else if (hasCustomSourceSets) {
      // can't be Android
      require(projectType != ProjectType.ANDROID) { "Expected JVM or KMP project" }
      if (projectType == ProjectType.JVM) {
        JvmSourceKind.of(variantSlug)
      } else {
        KmpSourceKind.of(variantSlug)
      }
    } else if (variantSlug == SourceKind.TEST_FIXTURES_NAME) {
      require(projectType == ProjectType.ANDROID) { "Expected Android project" }
      AndroidSourceKind.ANDROID_TEST_FIXTURES
    } else if (variantSlug.startsWith(SourceKind.TEST_FIXTURES_NAME)) {
      require(projectType == ProjectType.ANDROID) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.TEST_FIXTURES_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.testFixtures(name)
    } else if (variantSlug.startsWith(SourceKind.TEST_NAME)) {
      // must be Android
      require(projectType == ProjectType.ANDROID) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.TEST_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.test(name)
    } else if (variantSlug.startsWith(SourceKind.ANDROID_TEST_NAME)) {
      // must be Android
      require(projectType == ProjectType.ANDROID) { "Expected Android project" }
      val name = variantSlug
        .removePrefix(SourceKind.ANDROID_TEST_NAME)
        .replaceFirstChar(Char::lowercase)

      AndroidSourceKind.androidTest(name)
    } else {
      // TODO(tsr): when do we hit this case?
      when (projectType) {
        ProjectType.ANDROID -> AndroidSourceKind.main(variantSlug)
        ProjectType.JVM -> JvmSourceKind.of(variantSlug)
        ProjectType.KMP -> KmpSourceKind.of(variantSlug)
      }
    }
  }
}
