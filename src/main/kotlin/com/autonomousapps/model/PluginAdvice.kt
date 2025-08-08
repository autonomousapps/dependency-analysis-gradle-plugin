// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
public data class PluginAdvice(
  val redundantPlugin: String,
  val reason: String
) : Comparable<PluginAdvice> {

  override fun compareTo(other: PluginAdvice): Int {
    return redundantPlugin.compareTo(other.redundantPlugin)
  }

  public companion object {
    private const val JAVA_LIBRARY = "java-library"
    private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
    private const val KOTLIN_KAPT = "kotlin-kapt"

    @JvmStatic
    public fun redundantJavaLibrary(): PluginAdvice = PluginAdvice(
      redundantPlugin = JAVA_LIBRARY,
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove java-library"
    )

    @JvmStatic
    public fun redundantKotlinJvm(): PluginAdvice = PluginAdvice(
      redundantPlugin = KOTLIN_JVM,
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove org.jetbrains.kotlin.jvm"
    )

    @JvmStatic
    public fun redundantKapt(): PluginAdvice = PluginAdvice(
      redundantPlugin = KOTLIN_KAPT,
      reason = "this project has the kotlin-kapt (org.jetbrains.kotlin.kapt) plugin applied, but " +
        "there are no used annotation processors."
    )
  }
}
