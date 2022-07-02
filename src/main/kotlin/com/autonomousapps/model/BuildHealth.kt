package com.autonomousapps.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildHealth(
  val projectAdvice: Set<ProjectAdvice>,
  val shouldFail: Boolean,
  val unusedCount: Int,
  val undeclaredCount: Int,
  val misDeclaredCount: Int,
  val compileOnlyCount: Int,
  val runtimeOnlyCount: Int,
  val processorCount: Int,
)
