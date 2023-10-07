package com.autonomousapps.internal.kotlin

import org.gradle.api.Named
import org.gradle.api.attributes.*
import java.io.Serializable

/** Shaded copy of the real KGP [org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType] for classpath reasons. */
internal enum class KotlinPlatformType: Named, Serializable {
  common, jvm, js, androidJvm, native, wasm;

  override fun toString(): String = name
  override fun getName(): String = name

  companion object {
    val attribute = Attribute.of(
      "org.jetbrains.kotlin.platform.type",
      KotlinPlatformType::class.java
    )
  }
}
