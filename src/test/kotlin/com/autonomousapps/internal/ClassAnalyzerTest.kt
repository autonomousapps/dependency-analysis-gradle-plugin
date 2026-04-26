// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassReader
import com.autonomousapps.internal.fixtures.LambdaUsage
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
import com.google.common.truth.Truth.assertThat
import org.gradle.api.logging.Logging
import org.junit.jupiter.api.Test

class ClassAnalyzerTest {

  private val logger = Logging.getLogger(ClassAnalyzerTest::class.java)

  @Test fun `records lambda usage in binaryClassAccesses`() {
    val classAnalyzer = ClassAnalyzer(logger).also { analyzer ->
      ClassReader(LambdaUsage::class.java.name).accept(analyzer, 0)
    }

    val binaryClassAccesses: Map<String, Set<MemberAccess>> = classAnalyzer.getBinaryClasses()

    val methodOwner = "com/autonomousapps/internal/fixtures/ClassUsedInLambda"
    assertThat(binaryClassAccesses).containsKey(methodOwner)
    assertThat(binaryClassAccesses[methodOwner]).containsExactly(
      MemberAccess.Method(
        owner = methodOwner,
        name = "<init>",
        descriptor = "()V",
      ),
      MemberAccess.Method(
        owner = methodOwner,
        name = "someMethod",
        descriptor = "()V",
      )
    )
  }
}
