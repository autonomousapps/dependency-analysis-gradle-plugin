// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.google.common.truth.Truth.assertWithMessage
import org.junit.jupiter.api.Test

internal class DiscoverClasspathDuplicationTaskTest {

  @Test fun `recognizes Android R classes`() {
    val rClasses = listOf(
      "androidx/appcompat/R.class",
      "androidx/appcompat/R\$id.class",
      "androidx/appcompat/R\$styleable.class",
      "androidx/preference/R.class",
      "mozilla/components/browser/menu/R.class",
      "mozilla/components/feature/addons/R\$string.class",
      // Default package.
      "R.class",
    )

    rClasses.forEach {
      assertWithMessage(it).that(isAndroidRClass(it)).isTrue()
    }
  }

  @Test fun `does not flag other classes that merely start with R`() {
    val others = listOf(
      "com/example/MyClass.class",
      "androidx/appcompat/Rabbit.class",
      "com/example/MyR.class",
      "com/example/Resources.class",
      "com/example/RFoo.class",
      // Butterknife's R2 is intentionally not treated as an R class.
      "com/example/R2.class",
    )

    others.forEach {
      assertWithMessage(it).that(isAndroidRClass(it)).isFalse()
    }
  }
}
