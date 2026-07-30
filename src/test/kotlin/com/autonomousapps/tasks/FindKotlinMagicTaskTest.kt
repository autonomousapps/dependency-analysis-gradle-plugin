// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.TypealiasCapability
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

internal class FindKotlinMagicTaskTest {

  /**
   * `FindKotlinMagicTask` serves cache hits from the daemon and sends only the misses to its isolated worker, so its
   * reports are a merge of the two. Each dependency must be attributed to its own artifact's coordinates, whichever
   * side it came from.
   */
  @Test fun `merges cache hits with capabilities computed by the worker`() {
    val cachedArtifact = artifact("org.example:cached")
    val freshArtifact = artifact("org.example:fresh")

    val (inlineMembers, typealiases) = mergeKotlinMagic(
      artifacts = listOf(cachedArtifact, freshArtifact),
      cacheHits = mapOf(cachedArtifact.key to kotlinMagic("Cached")),
      newEntries = mapOf(freshArtifact.key to kotlinMagic("Fresh")),
    )

    assertThat(inlineMembers.map { it.coordinates })
      .containsExactly(cachedArtifact.coordinates, freshArtifact.coordinates)
    assertThat(inlineMembers.flatMap { dependency -> dependency.inlineMembers.map { it.className } })
      .containsExactly("com.example.CachedKt", "com.example.FreshKt")
    assertThat(typealiases.map { it.coordinates })
      .containsExactly(cachedArtifact.coordinates, freshArtifact.coordinates)
  }

  /** An artifact contributing neither inline members nor typealiases belongs in neither report. */
  @Test fun `artifacts without kotlin magic are left out of both reports`() {
    val bare = artifact("org.example:bare")
    val magic = artifact("org.example:magic")

    val (inlineMembers, typealiases) = mergeKotlinMagic(
      artifacts = listOf(bare, magic),
      cacheHits = mapOf(bare.key to KotlinCapabilities.EMPTY),
      newEntries = mapOf(magic.key to kotlinMagic("Magic")),
    )

    assertThat(inlineMembers.map { it.coordinates }).containsExactly(magic.coordinates)
    assertThat(typealiases.map { it.coordinates }).containsExactly(magic.coordinates)
  }

  /** The cache is keyed by artifact path, as in the task. */
  private val PhysicalArtifact.key: String get() = file.absolutePath

  /** [mergeKotlinMagic] reads only coordinates and the file path, so the jar itself need not exist on disk. */
  private fun artifact(identifier: String) = PhysicalArtifact(
    coordinates = ModuleCoordinates(identifier, "1", GradleVariantIdentification.EMPTY),
    file = File("${identifier.substringAfter(':')}.jar"),
  )

  private fun kotlinMagic(simpleName: String) = KotlinCapabilities(
    inlineMembers = setOf(
      InlineMemberCapability.InlineMember.newInstance(
        className = "com.example.${simpleName}Kt",
        packageName = "com.example",
        inlineMembers = setOf("com.example.thing"),
      )
    ),
    typealiases = setOf(
      TypealiasCapability.Typealias.newInstance(
        packageName = "com.example",
        alternatePackageName = "com.example",
        typealiases = setOf(TypealiasCapability.Typealias.Alias(simpleName, "kotlin.String")),
      )
    ),
  )
}
