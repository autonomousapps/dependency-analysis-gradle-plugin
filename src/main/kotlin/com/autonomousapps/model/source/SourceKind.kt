// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.source

import com.autonomousapps.internal.utils.capitalizeSafely
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.Serializable

/**
 * Representation of a source-set (JVM) or variant (Android) ([name]) in the project-under-analysis.
 *
 * This acts as a replacement for the old `com.autonomousapps.model.declaration.Variant` API.
 *
 * TODO(tsr): consider making `compileClasspathName` and `runtimeClasspathName` into functions to reduce size of
 *  serialized form. (I've tried twice and don't like it.)
 */
@JsonClass(generateAdapter = false, generator = "sealed:type")
public sealed class SourceKind : Comparable<SourceKind>, Serializable {
  /** Variant name for Android, or source set name for JVM or KMP. */
  public abstract val name: String

  /** MAIN, TEST, ANDROID_TEST, CUSTOM_JVM */
  public abstract val kind: String

  /** The name of the compile classpath configuration. */
  public abstract val compileClasspathName: String

  /** The name of the runtime classpath configuration. */
  public abstract val runtimeClasspathName: String

  /** Typically just `this` However, in the case of [AndroidSourceKind], strips the variant (flavor/buildType) away. */
  internal abstract fun base(): SourceKind

  /**
   * Returns true if [runtimeClasspathName] has a match in [classpaths]. Match need not be exact, e.g. in the case where
   * some classpaths extend others. At time of writing, this is only relevant for Android projects. JVM projects require
   * an exact match.
   */
  internal abstract fun runtimeMatches(classpaths: Collection<String>): Boolean

  /**
   * Returns true if [sourceSetName] matches [name], accounting for complexities in Android source sets that don't
   * permit straightforward equality checks.
   */
  internal abstract fun sourceSetMatches(sourceSetName: String): Boolean

  internal companion object {
    const val MAIN_NAME = "main"
    const val TEST_NAME = "test"
    const val TEST_FIXTURES_NAME = "testFixtures"
    const val ANDROID_TEST_NAME = "androidTest"

    const val MAIN_KIND = "MAIN"
    const val TEST_KIND = "TEST"
    const val ANDROID_TEST_FIXTURES_KIND = "ANDROID_TEST_FIXTURES"
    const val ANDROID_TEST_KIND = "ANDROID_TEST"
    const val CUSTOM_JVM_KIND = "CUSTOM_JVM"
  }
}

@TypeLabel("android")
@JsonClass(generateAdapter = false)
public data class AndroidSourceKind(
  override val name: String,
  override val kind: String,
  override val compileClasspathName: String,
  override val runtimeClasspathName: String,
) : SourceKind(), Serializable {

  override fun base(): AndroidSourceKind {
    return when (kind) {
      MAIN_KIND -> main(MAIN_NAME)
      TEST_KIND -> test(TEST_NAME)
      ANDROID_TEST_FIXTURES_KIND -> testFixtures(TEST_FIXTURES_NAME)
      ANDROID_TEST_KIND -> androidTest(ANDROID_TEST_NAME)
      else -> error("Expected one of 'main', 'test', or 'androidTest'. Was '$kind'.")
    }
  }

  override fun runtimeMatches(classpaths: Collection<String>): Boolean {
    return if (runtimeClasspathName in VIRTUAL_CLASSPATHS) {
      // We have a virtual SourceKind. This is used in StandardTransform for comparing classpaths.

      // debugFlavorCompileClasspath -> (filter out)
      // debugFlavorRuntimeClasspath -> debugFlavor
      // debugFlavorUnitTestRuntimeClasspath -> debugFlavorUnitTest
      // debugFlavorAndroidTestRuntimeClasspath -> debugFlavorAndroidTest
      val variants = classpaths
        .filterNot { it.endsWith("CompileClasspath") }
        .map { it.removeSuffix("RuntimeClasspath") }

      variants.any { m ->
        when {
          m.endsWith("UnitTest") -> kind == TEST_KIND
          m.endsWith("AndroidTest") -> kind == ANDROID_TEST_KIND
          m.endsWith("TestFixtures") -> kind == ANDROID_TEST_FIXTURES_KIND
          // Android app modules do something weird to the androidTest runtime classpaths, so we say that, if it's on a
          // main runtime classpath, it's also on the androidTest runtime classpath.
          else -> kind == MAIN_KIND || kind == ANDROID_TEST_KIND
        }
      }
    } else {
      // normal variant-specific classpath
      runtimeClasspathName in classpaths
    }
  }

  override fun sourceSetMatches(sourceSetName: String): Boolean {
    // debugRuntimeClasspath, debugUnitTestRuntimeClasspath, debugAndroidTest
    return sourceSetName == name
      || sourceSetName == base().name
      // sourceSetName=debugAndroidTest, name=debug, kind=ANDROID_TEST; base().name=androidTest. Therefore debugAndroidTest
      || sourceSetName == "$name${base().name.capitalizeSafely()}"
  }

  override fun compareTo(other: SourceKind): Int {
    if (other !is AndroidSourceKind) return -1

    return compareBy(SourceKind::name)
      .thenBy(SourceKind::kind)
      .thenBy(SourceKind::compileClasspathName)
      .thenBy(SourceKind::runtimeClasspathName)
      .compare(this, other)
  }

  internal companion object {
    val MAIN = main(MAIN_NAME)
    val TEST = test(TEST_NAME)
    val ANDROID_TEST = androidTest(ANDROID_TEST_NAME)
    val ANDROID_TEST_FIXTURES = testFixtures(TEST_FIXTURES_NAME)

    val VIRTUAL_CLASSPATHS = listOf(
      "runtimeClasspath",
      "unitTestRuntimeClasspath",
      "androidTestRuntimeClasspath",
      "testFixturesRuntimeClasspath",
    )

    fun main(variantName: String): AndroidSourceKind {
      return AndroidSourceKind(
        name = variantName,
        kind = MAIN_KIND,
        compileClasspathName = if (variantName == MAIN_NAME) {
          "compileClasspath"
        } else {
          "${variantName}CompileClasspath"
        },
        runtimeClasspathName = if (variantName == MAIN_NAME) {
          "runtimeClasspath"
        } else {
          "${variantName}RuntimeClasspath"
        },
      )
    }

    fun test(variantName: String): AndroidSourceKind {
      return AndroidSourceKind(
        name = variantName,
        kind = TEST_KIND,
        compileClasspathName = if (variantName == TEST_NAME) {
          "unitTestCompileClasspath"
        } else {
          "${variantName}UnitTestCompileClasspath"
        },
        runtimeClasspathName = if (variantName == TEST_NAME) {
          "unitTestRuntimeClasspath"
        } else {
          "${variantName}UnitTestRuntimeClasspath"
        },
      )
    }

    fun testFixtures(variantName: String): AndroidSourceKind {
      return AndroidSourceKind(
        name = variantName,
        kind = ANDROID_TEST_FIXTURES_KIND,
        compileClasspathName = if (variantName == TEST_FIXTURES_NAME) {
          "testFixturesCompileClasspath"
        } else {
          "${variantName}TestFixturesCompileClasspath"
        },
        runtimeClasspathName = if (variantName == TEST_FIXTURES_NAME) {
          "testFixturesRuntimeClasspath"
        } else {
          "${variantName}TestFixturesRuntimeClasspath"
        }
      )
    }

    fun androidTest(variantName: String): AndroidSourceKind {
      return AndroidSourceKind(
        name = variantName,
        kind = ANDROID_TEST_KIND,
        compileClasspathName = if (variantName == ANDROID_TEST_NAME) {
          "androidTestCompileClasspath"
        } else {
          "${variantName}AndroidTestCompileClasspath"
        },
        runtimeClasspathName = if (variantName == ANDROID_TEST_NAME) {
          "androidTestRuntimeClasspath"
        } else {
          "${variantName}AndroidTestRuntimeClasspath"
        },
      )
    }
  }
}

@TypeLabel("jvm")
@JsonClass(generateAdapter = false)
public data class JvmSourceKind(
  override val name: String,
  override val kind: String,
  override val compileClasspathName: String,
  override val runtimeClasspathName: String,
) : SourceKind(), Serializable {

  override fun base(): JvmSourceKind = this
  override fun runtimeMatches(classpaths: Collection<String>): Boolean = runtimeClasspathName in classpaths
  override fun sourceSetMatches(sourceSetName: String): Boolean = sourceSetName == name

  override fun compareTo(other: SourceKind): Int {
    if (other is AndroidSourceKind) return 1
    if (other is KmpSourceKind) return -1

    return compareBy(SourceKind::name)
      .thenBy(SourceKind::kind)
      .thenBy(SourceKind::compileClasspathName)
      .thenBy(SourceKind::runtimeClasspathName)
      .compare(this, other)
  }

  internal companion object {
    val MAIN = of(MAIN_NAME)
    val TEST = of(TEST_NAME)

    fun of(sourceSetName: String): JvmSourceKind {
      return JvmSourceKind(
        name = sourceSetName,
        kind = when (sourceSetName) {
          SourceSet.MAIN_SOURCE_SET_NAME -> MAIN_KIND
          SourceSet.TEST_SOURCE_SET_NAME -> TEST_KIND
          else -> CUSTOM_JVM_KIND
        },
        compileClasspathName = if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
          "compileClasspath"
        } else {
          "${sourceSetName}CompileClasspath"
        },
        runtimeClasspathName = if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
          "runtimeClasspath"
        } else {
          "${sourceSetName}RuntimeClasspath"
        },
      )
    }
  }
}

@TypeLabel("kmp")
@JsonClass(generateAdapter = false)
public data class KmpSourceKind(
  override val name: String,
  override val kind: String,
  override val compileClasspathName: String,
  override val runtimeClasspathName: String,
) : SourceKind(), Serializable {

  override fun base(): KmpSourceKind = this
  override fun runtimeMatches(classpaths: Collection<String>): Boolean = runtimeClasspathName in classpaths
  override fun sourceSetMatches(sourceSetName: String): Boolean = sourceSetName == name

  override fun compareTo(other: SourceKind): Int {
    if (other !is KmpSourceKind) return 1

    return compareBy(SourceKind::name)
      .thenBy(SourceKind::kind)
      .thenBy(SourceKind::compileClasspathName)
      .thenBy(SourceKind::runtimeClasspathName)
      .compare(this, other)
  }

  internal companion object {
    const val COMMON_MAIN_NAME = "commonMain"
    const val COMMON_TEST_NAME = "commonTest"
    const val JVM_MAIN_NAME = "jvmMain"
    const val JVM_TEST_NAME = "jvmTest"

    val JVM_MAIN = of(JVM_MAIN_NAME)
    val JVM_TEST = of(JVM_TEST_NAME)

    fun of(compilation: KotlinCompilation<*>): KmpSourceKind {
      val runtimeClasspathName = compilation.runtimeDependencyConfigurationName
        ?: error("Kotlin compilation ${compilation.name} has null 'runtimeDependencyConfigurationName'")

      return KmpSourceKind(
        name = compilation.defaultSourceSet.name,
        // Always custom
        kind = CUSTOM_JVM_KIND,
        compileClasspathName = compilation.compileDependencyConfigurationName,
        runtimeClasspathName = runtimeClasspathName,
      )
    }

    fun of(sourceSetName: String): KmpSourceKind {
      return KmpSourceKind(
        name = sourceSetName,
        // Always custom
        kind = CUSTOM_JVM_KIND,
        compileClasspathName = if (sourceSetName == JVM_MAIN_NAME) {
          // jvmMain => jvmCompileClasspath (jvmMain is special)
          "jvmCompileClasspath"
        } else {
          // jvmTest => jvmTestCompileClasspath
          "${sourceSetName}CompileClasspath"
        },
        runtimeClasspathName = if (sourceSetName == JVM_MAIN_NAME) {
          // jvmMain => jvmRuntimeClasspath (jvmMain is special)
          "jvmRuntimeClasspath"
        } else {
          // jvmTest => jvmTestRuntimeClasspath
          "${sourceSetName}RuntimeClasspath"
        },
      )
    }
  }
}
