package com.autonomousapps.model

data class BuildHealth(
  val projectAdvice: Set<ProjectAdvice>,
  val shouldFail: Boolean,
  val unusedCount: Int,
  val undeclaredCount: Int,
  val misDeclaredCount: Int
)
