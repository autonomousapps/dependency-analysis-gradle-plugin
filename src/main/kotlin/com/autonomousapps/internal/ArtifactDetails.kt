// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import java.io.File

/** Only public because it's used as a task input. Must be used with `@Nested`. */
public data class ArtifactDetails(
  @get:Input val id: ComponentArtifactIdentifier,
  @get:Input val variant: ResolvedVariantResult,
) {
  internal companion object {
    fun of(artifacts: Provider<Set<ResolvedArtifactResult>>): Provider<List<ArtifactDetails>> {
      return artifacts.map { it.map { artifact -> ArtifactDetails(artifact.id, artifact.variant) } }
    }

    fun files(artifacts: Provider<Set<ResolvedArtifactResult>>): Provider<List<File>> {
      return artifacts.map { it.map { artifact -> artifact.file } }
    }

    fun sequenced(
      details: ListProperty<ArtifactDetails>,
      files: ListProperty<File>,
    ): Sequence<Pair<ArtifactDetails, File>> = zipped(details, files).asSequence()

    fun zipped(details: ListProperty<ArtifactDetails>, files: ListProperty<File>): List<Pair<ArtifactDetails, File>> {
      val details = details.get()
      val files = files.get()
      require(details.size == files.size) {
        "Expected 'details.size == files.size'. Got details.size=${details.size}, files.size=${files.size}"
      }

      return details.zip(files)
    }
  }
}
