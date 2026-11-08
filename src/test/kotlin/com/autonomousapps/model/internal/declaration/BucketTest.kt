// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class BucketTest {
  @Test fun `API is 'less than' IMPL`() {
    assertThat(minOf(Bucket.API, Bucket.IMPL)).isEqualTo(Bucket.API)
    assertThat(Bucket.API.highest(Bucket.IMPL)).isEqualTo(Bucket.API)
    assertThat(Bucket.IMPL.highest(Bucket.API)).isEqualTo(Bucket.API)
  }
}
