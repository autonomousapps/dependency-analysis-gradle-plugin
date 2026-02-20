// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.kit.gradle.kotlin.KotlinSourceSets
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

// TODO(tsr): consider moving to .kotlin package (breaking change)
//  Should probably deprecate because I want to change other parts of this too.
public class Kotlin @JvmOverloads constructor(
  private val jvmToolchain: JvmToolchain? = JvmToolchain.DEFAULT,
  private val jvmTarget: KotlinJvmTarget? = null,
  private val sourceSets: KotlinSourceSets? = null,
) : Element.Block {

  override val name: String = "kotlin"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    jvmToolchain?.render(s)
    jvmTarget?.render(s)
    sourceSets?.render(s)
  }

  public companion object {
    @JvmField
    public val DEFAULT: Kotlin = Kotlin()

    @JvmStatic
    public fun ofTarget(target: Int): Kotlin = Kotlin(JvmToolchain(target))

    @JvmStatic
    public fun of(
      jvmTarget: KotlinJvmTarget,
      sourceSets: KotlinSourceSets,
    ): Kotlin {
      return Kotlin(
        jvmToolchain = null,
        jvmTarget = jvmTarget,
        sourceSets = sourceSets,
      )
    }

    @JvmStatic
    public fun of(
      jvmToolchainTarget: Int,
      jvmTarget: KotlinJvmTarget,
      sourceSets: KotlinSourceSets,
    ): Kotlin {
      return Kotlin(
        jvmToolchain = JvmToolchain(jvmToolchainTarget),
        jvmTarget = jvmTarget,
        sourceSets = sourceSets,
      )
    }
  }
}
