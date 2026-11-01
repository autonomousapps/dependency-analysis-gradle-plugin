// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.kotlin.ClassBinarySignature
import com.autonomousapps.model.internal.AccessFlags
import com.autonomousapps.model.internal.kmp.SourceSetFiles.Companion.findSourceSetName
import com.google.common.truth.Truth.assertThat
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
    )
  )

  @Test fun `can find source set name in checked-in file in non-root package`() {
    val classSignature = classSignature("consumer/android/main/Platform")
    assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("androidMain")
  }

  @Test fun `can find source set name in checked-in file in root package`() {
    val classSignature = classSignature("Foo")
    assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("androidMain")
  }

  @Test fun `can find source set name in generated file in non-root package`() {
    val classSignature = classSignature("a/b/GeneratedKt")
    assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("commonMain")
  }

  @Test fun `can find source set name in generated file in root package`() {
    val classSignature = classSignature("GeneratedKt")
    assertThat(sourceSetFiles.findSourceSetName(classSignature)).isEqualTo("commonMain")
  }

  private fun classSignature(name: String): ClassBinarySignature {
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
    )
  }
}
