// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.gradle.android.AndroidLibraryTarget
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.kit.gradle.kotlin.KotlinSourceSets
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

// TODO(tsr): consider moving to .kotlin package (breaking change)
//  Should probably deprecate because I want to change other parts of this too.
public class Kotlin @JvmOverloads constructor(
  private val jvmToolchain: JvmToolchain? = JvmToolchain.DEFAULT,
  private val androidLibraryTarget: AndroidLibraryTarget? = null,
  private val jvmTarget: KotlinJvmTarget? = null,
  private val sourceSets: KotlinSourceSets? = null,
) : Element.Block {

  override val name: String = "kotlin"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    jvmToolchain?.render(s)
    jvmTarget?.render(s)
    androidLibraryTarget?.render(s)
    sourceSets?.render(s)
  }

  public class Builder {
    public var jvmToolchain: JvmToolchain? = null

    public var androidLibraryTarget: AndroidLibraryTarget? = null
    public var jvmTarget: KotlinJvmTarget? = null

    private var androidLibraryTargetBuilder: AndroidLibraryTarget.Builder? = null
    private var sourceSetsBuilder: KotlinSourceSets.Builder? = null

    public fun androidLibrary(block: (AndroidLibraryTarget.Builder) -> Unit) {
      val androidLibraryTargetBuilder = androidLibraryTargetBuilder ?: AndroidLibraryTarget.Builder()
      block(androidLibraryTargetBuilder)
      this.androidLibraryTargetBuilder = androidLibraryTargetBuilder
    }

    public fun sourceSets(block: (KotlinSourceSets.Builder) -> Unit) {
      val sourceSetsBuilder = sourceSetsBuilder ?: KotlinSourceSets.Builder()
      block(sourceSetsBuilder)
      this.sourceSetsBuilder = sourceSetsBuilder
    }

    public fun build(): Kotlin {
      return Kotlin(
        jvmToolchain = jvmToolchain,
        androidLibraryTarget = androidLibraryTargetBuilder?.build(),
        jvmTarget = jvmTarget,
        sourceSets = sourceSetsBuilder?.build(),
      )
    }
  }

  public companion object {
    /** Deprecated. Use Kotlin.Builder instead. */
    @Deprecated("Use Kotlin.Builder instead.")
    @JvmField
    public val DEFAULT: Kotlin = Kotlin()

    /**
     * This is the JDK target.
     *
     * Deprecated. Use Kotlin.Builder instead.
     */
    @Deprecated("Use Kotlin.Builder instead.")
    @JvmStatic
    public fun ofTarget(target: Int): Kotlin = Kotlin(JvmToolchain(target))
  }
}
