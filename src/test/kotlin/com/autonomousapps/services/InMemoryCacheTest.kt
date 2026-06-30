// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class InMemoryCacheTest {

  @Test fun `cache with maximumSize evicts entries beyond limit`() {
    val cache = Caffeine.newBuilder()
      .maximumSize(3)
      .build<String, String>()

    cache.put("a", "1")
    cache.put("b", "2")
    cache.put("c", "3")
    cache.put("d", "4")
    cache.cleanUp()

    assertThat(cache.estimatedSize()).isEqualTo(3)
  }

  @Test fun `cache with maximumSize 300 accepts 300 entries`() {
    val cache = Caffeine.newBuilder()
      .maximumSize(300)
      .build<String, String>()

    repeat(300) { i ->
      cache.put("key-$i", "value-$i")
    }
    cache.cleanUp()

    assertThat(cache.estimatedSize()).isEqualTo(300)
  }
}
