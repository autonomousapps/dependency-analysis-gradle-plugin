// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.intermediates.AndroidScoreVariant
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class ModuleAdvice {

  abstract val name: String

  internal fun shouldIgnore(behavior: Behavior): Boolean {
    return behavior.filter.contains(name)
  }

  internal abstract fun isActionable(): Boolean

  internal companion object {
    /** Returns `true` if [moduleAdvice] is effectively empty or unactionable. */
    fun isEmpty(moduleAdvice: Set<ModuleAdvice>) = moduleAdvice.none { it.isActionable() }


    /** Returns `true` if [moduleAdvice] is in any way actionable. */
    fun isNotEmpty(moduleAdvice: Set<ModuleAdvice>) = !isEmpty(moduleAdvice)
  }
}

@TypeLabel("android_score")
@JsonClass(generateAdapter = false)
data class AndroidScore(
  val hasAndroidAssets: Boolean,
  val hasAndroidRes: Boolean,
  val usesAndroidClasses: Boolean,
  val hasBuildConfig: Boolean,
  val hasAndroidDependencies: Boolean,
) : ModuleAdvice() {

  override val name: String = "android"

  @delegate:Transient
  private val score: Float by unsafeLazy {
    var count = 0f
    if (hasAndroidAssets) count += 2
    if (hasAndroidRes) count += 2
    if (usesAndroidClasses) count += 2
    if (hasBuildConfig) count += 0.5f
    if (hasAndroidDependencies) count += 0.5f
    count
  }

  /** True if this project uses no Android facilities at all. */
  fun shouldBeJvm(): Boolean = score == 0f

  /** True if this project only uses some limited number of Android facilities. */
  fun couldBeJvm(): Boolean = score < THRESHOLD

  override fun isActionable(): Boolean = couldBeJvm()

  internal companion object {
    private const val THRESHOLD = 2f

    fun ofVariants(scores: Collection<AndroidScoreVariant>): AndroidScore? {
      // JVM projects don't have an AndroidScore
      if (scores.isEmpty()) return null

      var hasAndroidAssets = false
      var hasAndroidRes = false
      var hasBuildConfig = false
      var usesAndroidClasses = false
      var hasAndroidDependencies = false

      scores.forEach {
        hasAndroidAssets = hasAndroidDependencies || it.hasAndroidAssets
        hasAndroidRes = hasAndroidRes || it.hasAndroidRes
        hasBuildConfig = hasBuildConfig || it.hasBuildConfig
        usesAndroidClasses = usesAndroidClasses || it.usesAndroidClasses
        hasAndroidDependencies = hasAndroidDependencies || it.hasAndroidDependencies
      }

      return AndroidScore(
        hasAndroidAssets = hasAndroidAssets,
        hasAndroidRes = hasAndroidRes,
        hasBuildConfig = hasBuildConfig,
        usesAndroidClasses = usesAndroidClasses,
        hasAndroidDependencies = hasAndroidDependencies,
      )
    }
  }
}
