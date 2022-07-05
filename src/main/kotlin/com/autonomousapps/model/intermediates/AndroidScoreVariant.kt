package com.autonomousapps.model.intermediates

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
)
