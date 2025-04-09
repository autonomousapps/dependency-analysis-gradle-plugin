// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe
import org.intellij.lang.annotations.Language

/**
 * The `android` block, for use by projects build with the Android Gradle Plugin.
 * ```
 * // build.gradle[.kts]
 * android {
 *   ...
 * }
 * ```
 */
public class AndroidBlock @JvmOverloads constructor(
  public var namespace: String? = null,
  public var compileSdkVersion: Int = 34,
  public var defaultConfig: DefaultConfig = DefaultConfig.DEFAULT_APP,
  public var compileOptions: CompileOptions = CompileOptions.DEFAULT,
  public var kotlinOptions: KotlinOptions? = null,
  /** Used by `com.android.test` projects */
  public var targetProjectPath: String? = null,
  public var additions: String = "",
  private val usesGroovy: Boolean = false,
  private val usesKotlin: Boolean = false,
) : Element.Block {

  override val name: String = "android"

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.block(this) { s ->
    if (namespace != null) {
      s.line {
        it.append("namespace '")
        it.append(namespace)
        it.append("'")
      }
    }
    if (targetProjectPath != null) {
      s.line {
        it.append("targetProjectPath = '")
        it.append(targetProjectPath)
        it.append("'")
      }
    }
    s.line {
      it.append("compileSdkVersion ")
      it.append(compileSdkVersion)
    }
    defaultConfig.render(s)
    compileOptions.render(s)
    kotlinOptions?.render(s)
    
    if (additions.isNotBlank()) {
      if (usesKotlin) {
        error("You called withGroovy() but you're using Kotlin DSL")
      }
      s.line { it.append(additions) }
    }
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.block(this) { s ->
    if (namespace != null) {
      s.line {
        it.append("namespace = \"")
        it.append(namespace)
        it.append("\"")
      }
    }
    if (targetProjectPath != null) {
      s.line {
        it.append("targetProjectPath = \"")
        it.append(targetProjectPath)
        it.append("\"")
      }
    }
    s.line {
      it.append("compileSdk = ")
      it.append(compileSdkVersion)
    }
    defaultConfig.render(s)
    compileOptions.render(s)
    kotlinOptions?.render(s)

    if (additions.isNotBlank()) {
      if (usesGroovy) {
        error("You called withKotlin() but you're using Groovy DSL")
      }
      s.line { it.append(additions) }
    }
  }

  public class Builder {
    public var namespace: String? = null
    public var compileSdkVersion: Int = 34
    public var defaultConfig: DefaultConfig = DefaultConfig.DEFAULT_APP
    public var compileOptions: CompileOptions = CompileOptions.DEFAULT
    public var kotlinOptions: KotlinOptions? = null

    public var additions: String = ""
    private var usesGroovy = false
    private var usesKotlin = false

    public fun withGroovy(@Language("Groovy") script: String) {
      additions = script.trimIndent()
      usesGroovy = true
    }

    public fun withKotlin(@Language("kt") script: String) {
      additions = script.trimIndent()
      usesKotlin = true
    }

    public fun build(): AndroidBlock {
      return AndroidBlock(
        namespace = namespace,
        compileSdkVersion = compileSdkVersion,
        defaultConfig = defaultConfig,
        compileOptions = compileOptions,
        kotlinOptions = kotlinOptions,
        additions = additions,
      )
    }
  }

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun defaultAndroidAppBlock(
      isKotlinApplied: Boolean = false,
      namespace: String? = null,
    ): AndroidBlock = AndroidBlock(
      namespace = namespace,
      defaultConfig = DefaultConfig.DEFAULT_APP,
      kotlinOptions = if (isKotlinApplied) KotlinOptions.DEFAULT else null
    )

    @JvmOverloads
    @JvmStatic
    public fun defaultAndroidLibBlock(
      isKotlinApplied: Boolean = false,
      namespace: String? = null,
    ): AndroidBlock = AndroidBlock(
      namespace = namespace,
      defaultConfig = DefaultConfig.DEFAULT_LIB,
      kotlinOptions = if (isKotlinApplied) KotlinOptions.DEFAULT else null
    )

    @JvmOverloads
    @JvmStatic
    public fun defaultAndroidTestBlock(
      targetProjectPath: String,
      isKotlinApplied: Boolean = false,
      namespace: String? = null,
    ): AndroidBlock = AndroidBlock(
      namespace = namespace,
      targetProjectPath = targetProjectPath,
      defaultConfig = DefaultConfig.DEFAULT_TEST,
      kotlinOptions = if (isKotlinApplied) KotlinOptions.DEFAULT else null
    )
  }
}
