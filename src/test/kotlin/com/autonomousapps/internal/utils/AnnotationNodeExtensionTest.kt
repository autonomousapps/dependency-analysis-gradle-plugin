// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.internal.asm.Type
import com.autonomousapps.internal.asm.tree.AnnotationNode
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class AnnotationNodeExtensionTest {

  @Test fun `should correctly parse an annotation node that references a list of Types`() {
    val annotationNode = AnnotationNode("Lcom/test/AnnotationWithMultipleReferences;")
    annotationNode.values = mutableListOf()
    annotationNode.values.add(
      arrayListOf(
        "a string element that should be filtered out",
        Type.getType("Lcom/test/AReferencedClass;"),
        Type.getType("Lcom/test/AnotherOne;")
      )
    )

    val annotationTypes = listOf(annotationNode).annotationTypes()
    assertThat(annotationTypes).hasSize(3)
    assertThat(annotationTypes).containsExactly(
      "Lcom/test/AnnotationWithMultipleReferences;", "Lcom/test/AReferencedClass;", "Lcom/test/AnotherOne;"
    )
  }

  @Test fun `should correctly parse an annotation node that references a single Type`() {
    val annotationNode = AnnotationNode("Lcom/test/AnnotationWithSingleReference;")
    annotationNode.values = mutableListOf()
    annotationNode.values.add(
      Type.getType("Lcom/test/AReferencedClass;")
    )

    val annotationTypes = listOf(annotationNode).annotationTypes()
    assertThat(annotationTypes).hasSize(2)
    assertThat(annotationTypes).containsExactly(
      "Lcom/test/AnnotationWithSingleReference;", "Lcom/test/AReferencedClass;"
    )
  }

  @Test fun `should correctly parse an annotation node with no references`() {
    val annotationNode = AnnotationNode("Lcom/test/AnnotationWithNoReference;")
    val annotationTypes = listOf(annotationNode).annotationTypes()
    assertThat(annotationTypes).hasSize(1)
    assertThat(annotationTypes).containsExactly(
      "Lcom/test/AnnotationWithNoReference;"
    )
  }
}
