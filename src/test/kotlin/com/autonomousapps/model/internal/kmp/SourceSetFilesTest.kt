// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.ClassAnalyzer
import com.autonomousapps.internal.kotlin.ClassBinarySignature
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.model.internal.AccessFlags
import com.autonomousapps.model.internal.kmp.SourceSetFiles.Companion.findSourceSetName
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SourceSetFilesTest {

  private val sourceSetFiles = setOf(
    SourceSetFiles(
      sourceSetName = "androidMain",
      sourceBase = "src/androidMain/kotlin",
      sourceFiles = setOf("consumer/android/main/Platform.kt", "Foo.kt"),
    ),
    SourceSetFiles(
      sourceSetName = "commonMain",
      sourceBase = "build/gen/kotlin",
      sourceFiles = setOf("a/b/generated.kt", "generated.kt", "producer/common/main/Shout.kt"),
    ),
    SourceSetFiles(
      sourceSetName = "commonMain",
      sourceBase = "src/commonMain/kotlin",
      sourceFiles = setOf("consumer/common/main/Values.kt"),
    ),
  )

  private val duplicateSourceSetFiles = setOf(
    SourceSetFiles(
      sourceSetName = "androidMain",
      sourceBase = "src/androidMain/kotlin",
      sourceFiles = setOf("r/V.kt"),
    ),
    SourceSetFiles(
      sourceSetName = "jvmMain",
      sourceBase = "src/jvmMain/kotlin",
      sourceFiles = setOf("r/V.kt"),
    ),
  )

  @Nested inner class FromClassBinarySignature {
    @Test fun `can find source set name in checked-in file in non-root package`() {
      // not a typo. androidMain source set (build/classes/kotlin/android/main/) and android.main package (.../android/main/Platform.class)
      val classSignature = classSignature(
        "consumer/android/main/Platform",
        "build/classes/kotlin/android/main/android/main/Platform.class"
      )
      assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("androidMain")
    }

    @Test fun `can find source set name in checked-in file in root package`() {
      val classSignature = classSignature("Foo", "build/classes/kotlin/android/main/Foo.class")
      assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("androidMain")
    }

    @Test fun `can find source set name in generated file in non-root package`() {
      // TODO(tsr): validate class file path for generated source code
      val classSignature =
        classSignature("a/b/GeneratedKt", "build/classes/kotlin/common/main/a/b/GeneratedKt.class")
      assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("commonMain")
    }

    @Test fun `can find source set name from class signature in generated file in root package`() {
      val classSignature =
        classSignature("GeneratedKt", "build/classes/kotlin/common/main/GeneratedKt.class")
      assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("commonMain")
    }

    @Test fun `can find source set name from class signature when there are duplicate sources`() {
      val classSignature = classSignature("r/VKt", "build/classes/kotlin/android/main/VKt.class")
      assertThat(duplicateSourceSetFiles.findSourceSetName(classSignature)).isEqualTo("androidMain")
    }

    private fun classSignature(
      name: String,
      sourceFilePath: String,
    ): ClassBinarySignature {
      return ClassBinarySignature(
        name = name,
        superName = "java/lang/Object",
        outerName = null,
        supertypes = emptyList(),
        genericTypes = emptySet(),
        memberSignatures = emptyList(),
        access = AccessFlags.PUBLIC,
        isEffectivelyPublic = true,
        isNotUsedWhenEmpty = true,
        annotations = emptySet(),
        invisibleAnnotations = emptySet(),
        sourceFile = "doesn't matter",
        classFilePath = sourceFilePath,
      )
    }
  }

  @Nested inner class FromClassAnalyzer {
    /**
     * Matches the source file:
     * ```
     * import kotlinx.coroutines.flow.Flow
     * import kotlinx.coroutines.flow.flowOf
     * import kotlinx.coroutines.flow.map
     * import producer.common.main.shout
     *
     * fun values(): Flow<String> = flowOf("a").map { shout(it) }
     * ```
     */
    @Test fun `can find source set name in inlined lambda`() {
      val classAnalyzer = classAnalyzer("consumer/common/main/ValuesKt\$values$\$inlined\$map$1$2$1")
      val relativePath = "classes/kotlin/common/main/r/ValuesKt.class"
      assertThat(sourceSetFiles.findSourceSetName(classAnalyzer, relativePath)).isEqualTo("commonMain")
    }

    /**
     * Identical except path on disk:
     * ```
     * src/androidMain/r/V.kt
     * src/jvmMain/r/V.kt
     * ```
     */
    @Test fun `can find source set name when there are duplicate sources`() {
      val classAnalyzer = classAnalyzer(className = "r/VKt", source = "V.kt")
      val relativePath = "classes/kotlin/jvm/main/r/VKt.class"
      assertThat(duplicateSourceSetFiles.findSourceSetName(classAnalyzer, relativePath)).isEqualTo("jvmMain")
    }

    private fun classAnalyzer(className: String, source: String? = null): ClassAnalyzer {
      return ClassAnalyzer(getLogger<SourceSetFilesTest>()).apply {
        this.className = className
        this.source = source
      }
    }
  }
}
