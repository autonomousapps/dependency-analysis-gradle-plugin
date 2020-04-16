package com.autonomousapps.advice

data class PluginAdvice(
  val redundantPlugin: String,
  val reason: String
) : Comparable<PluginAdvice> {

  companion object {
    // TODO make more flexible
    fun redundantPlugin() = PluginAdvice(
      redundantPlugin = "java-library",
      reason = "This project has both java-library and org.jetbrains.kotlin.jvm applied, which is redundant. You can remove java-library"
    )
  }

  override fun compareTo(other: PluginAdvice): Int {
    return redundantPlugin.compareTo(other.redundantPlugin)
  }
}
