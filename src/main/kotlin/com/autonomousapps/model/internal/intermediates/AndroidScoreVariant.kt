// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class AndroidScoreVariant(
  val variant: Variant,
  val hasAndroidAssets: Boolean,
  val hasAndroidRes: Boolean,
  val hasBuildConfig: Boolean,
  val usesAndroidClasses: Boolean,
  val hasAndroidDependencies: Boolean,
  val hasBuildTypeSourceSplits: Boolean,
)
