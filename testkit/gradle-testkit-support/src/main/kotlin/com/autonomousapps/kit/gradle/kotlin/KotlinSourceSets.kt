// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinSourceSets(
  private val sourceSets: List<KotlinSourceSet>,
) : Element.Block {

  override val name: String = "sourceSets"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    sourceSets.forEach { sourceSet ->
      sourceSet.render(s)
    }
  }

  public class Builder {
    public var sourceSets: List<KotlinSourceSet>? = null
    private val sourceSetsBuilders = mutableMapOf<String, KotlinSourceSet.Builder>()

    /** Configure the default "commonMain" source set. */
    public fun commonMain(block: (KotlinSourceSet.Builder) -> Unit) {
      named("commonMain", block)
    }

    /** Configure the default "commonTest" source set. */
    public fun commonTest(block: (KotlinSourceSet.Builder) -> Unit) {
      named("commonTest", block)
    }

    /** Configure the default "jvmMain" source set. */
    public fun jvmMain(block: (KotlinSourceSet.Builder) -> Unit) {
      named("jvmMain", block)
    }

    /** Configure the default "jvmTest" source set. */
    public fun jvmTest(block: (KotlinSourceSet.Builder) -> Unit) {
      named("jvmTest", block)
    }

    /** Create and configure a custom source set named [name]. */
    public fun named(name: String, block: (KotlinSourceSet.Builder) -> Unit) {
      val builder = sourceSetsBuilders.computeIfAbsent(name) { KotlinSourceSet.Builder(name) }
      block(builder)
    }

    public fun build(): KotlinSourceSets {
      return KotlinSourceSets(
        sourceSets = sourceSetsBuilders.values.map { it.build() },
      )
    }
  }
}
