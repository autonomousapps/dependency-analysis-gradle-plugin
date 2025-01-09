// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class BuildHealth(
  val projectAdvice: Set<ProjectAdvice>,
  val shouldFail: Boolean,
  val projectCount: Int,
  val unusedCount: Int,
  val undeclaredCount: Int,
  val misDeclaredCount: Int,
  val compileOnlyCount: Int,
  val runtimeOnlyCount: Int,
  val processorCount: Int,
  val androidScoreMetrics: AndroidScoreMetrics,
) {

  @JsonClass(generateAdapter = false)
  data class AndroidScoreMetrics(
    val shouldBeJvmCount: Int,
    val couldBeJvmCount: Int,
  ) {
    class Builder {
      var shouldBeJvmCount: Int = 0
      var couldBeJvmCount: Int = 0

      fun build() = AndroidScoreMetrics(
        shouldBeJvmCount = shouldBeJvmCount,
        couldBeJvmCount = couldBeJvmCount
      )
    }
  }
}
