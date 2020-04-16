package com.autonomousapps.advice

import org.gradle.api.Incubating

@Incubating
data class PluginAdvice(
  val redundantPlugin: String,
  val reason: String
) : Comparable<PluginAdvice> {

  companion object {
    fun redundantJavaLibrary() = PluginAdvice(
      redundantPlugin = "java-library",
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which is redundant. You can remove java-library"
    )

    fun redundantKotlinJvm() = PluginAdvice(
      redundantPlugin = "org.jetbrains.kotlin.jvm",
      reason = "this project has both java-library and org.jetbrains.kotlin.jvm applied, which is redundant. You can remove org.jetbrains.kotlin.jvm"
    )
  }

  override fun compareTo(other: PluginAdvice): Int {
    return redundantPlugin.compareTo(other.redundantPlugin)
  }
}
