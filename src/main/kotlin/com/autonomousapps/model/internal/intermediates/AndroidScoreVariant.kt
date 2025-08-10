// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class AndroidScoreVariant(
  val sourceKind: SourceKind,
  val hasAndroidAssets: Boolean,
  val hasAndroidRes: Boolean,
  val hasBuildConfig: Boolean,
  val usesAndroidClasses: Boolean,
  val hasAndroidDependencies: Boolean,
  val hasBuildTypeSourceSplits: Boolean,
)
