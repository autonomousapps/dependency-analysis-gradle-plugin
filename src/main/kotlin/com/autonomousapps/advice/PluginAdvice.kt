package com.autonomousapps.advice

import org.gradle.api.Incubating

@Incubating
data class PluginAdvice(
  val redundantPlugin: String,
  val reason: String
) : Comparable<PluginAdvice> {

  companion object {
    @JvmStatic
    fun redundantJavaLibrary() = PluginAdvice(
      redundantPlugin = "java-library",
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove java-library"
    )

    @JvmStatic
    fun redundantKotlinJvm() = PluginAdvice(
      redundantPlugin = "org.jetbrains.kotlin.jvm",
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which " +
        "is redundant. You can remove org.jetbrains.kotlin.jvm"
    )

    fun redundantKapt() = PluginAdvice(
      redundantPlugin = "kotlin-kapt",
      reason = "this project has the kotlin-kapt (org.jetbrains.kotlin.kapt) plugin applied, but " +
        "no annotation processors (or no used annotation processors), which is redundant."
    )
  }

  override fun compareTo(other: PluginAdvice): Int {
    return redundantPlugin.compareTo(other.redundantPlugin)
  }
}
