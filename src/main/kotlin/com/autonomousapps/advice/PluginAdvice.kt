// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.advice

import com.squareup.moshi.JsonClass

// TODO(2.0) move to com.autonomousapps.model package (breaking ABI change)
@JsonClass(generateAdapter = false)
data class PluginAdvice(
  val redundantPlugin: String,
  val reason: String
) : Comparable<PluginAdvice> {

  companion object {
    private const val JAVA_LIBRARY = "java-library"
    private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
    private const val KOTLIN_KAPT = "kotlin-kapt"

    @JvmStatic
    fun redundantJavaLibrary() = PluginAdvice(
      redundantPlugin = JAVA_LIBRARY,
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove java-library"
    )

    @JvmStatic
    fun redundantKotlinJvm() = PluginAdvice(
      redundantPlugin = KOTLIN_JVM,
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove org.jetbrains.kotlin.jvm"
    )

    @JvmStatic
    fun redundantKapt() = PluginAdvice(
      redundantPlugin = KOTLIN_KAPT,
      reason = "this project has the kotlin-kapt (org.jetbrains.kotlin.kapt) plugin applied, but " +
        "there are no used annotation processors."
    )
  }

  override fun compareTo(other: PluginAdvice): Int {
    return redundantPlugin.compareTo(other.redundantPlugin)
  }
}
