// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * In an Android KMP library:
 * ```
 * kotlin {
 *   androidLibrary {
 *     // This block
 *     compilerOptions.configure {
 *        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
 *     }
 *   }
 * }
 * ```
 * @see <a href="https://developer.android.com/kotlin/multiplatform/plugin">Android KMP library configuration</a>
 */
public class CompilerOptions(
  private val compilerJvmTarget: CompilerJvmTarget,
) : Element.Block {

  override val name: String = "compilerOptions"

  override fun render(scribe: Scribe): String = scribe.block("${name}.configure") { s ->
    compilerJvmTarget.render(s)
  }

  public companion object {
    @JvmStatic
    public fun of(jvmTarget: Int): CompilerOptions {
      return CompilerOptions(CompilerJvmTarget(jvmTarget))
    }
  }
}
