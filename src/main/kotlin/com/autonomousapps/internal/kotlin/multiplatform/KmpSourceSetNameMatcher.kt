// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind

internal object KmpSourceSetNameMatcher {
  /**
   * THIS IS ~WRONG AND SHOULD BE IMPROVED.
   *
   * Given the potential for arbitrary source set hierarchies, we want to search through the class references (`this`) for
   * the *root-most* one. However, as far as I know, KGP does not expose the source set tree in any API. So for now, we
   * rely on a heuristic. In the future, we may ask users for a priority list.
   *
   * For the moment, we start with `commonMain`, then look for anything starting with `common` and ending with `Main`,
   * then look for anything starting with `common` and ending with `Test`, and finally look for`commonTest`.
   *
   * @see <a href="https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html#modifying-source-sets">Modifying source sets</a>
   */
  fun getRootMostSourceKind(classReferences: Set<ClassReference>, fallback: SourceKind): SourceKind {
    // If not a KMP project, use the fallback
    if (fallback !is KmpSourceKind) return fallback

    // commonMain, commonJvmMain, commonJvmTest, commonTest
    val commonClassReference = classReferences.find { it.kmpSourceSetName == KmpSourceKind.COMMON_MAIN_NAME }
      ?: classReferences.find { it.kmpSourceSetName?.startsWith("common") == true && it.kmpSourceSetName.endsWith("Main") }
      ?: classReferences.find { it.kmpSourceSetName?.startsWith("common") == true && it.kmpSourceSetName.endsWith("Test") }
      ?: classReferences.find { it.kmpSourceSetName == KmpSourceKind.COMMON_TEST_NAME }
    val kmpCommonSourceSetName = commonClassReference?.kmpSourceSetName

    return if (kmpCommonSourceSetName != null) {
      // This keeps the runtimeClasspath the same, for example. That value is used later in the advice computation.
      fallback.copy(name = kmpCommonSourceSetName)
    } else {
      fallback
    }
  }
}
