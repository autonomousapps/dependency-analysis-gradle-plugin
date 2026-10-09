// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.provider.Provider
import java.io.File

/**
 * A decomposed version of a [ResolvedArtifactResult], suitable for task input wiring. Importantly, works in Gradle
 * 8.11, which this plugin supports at time of writing.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/artifact_resolution.html#sec:resolving_artifacts">Resolving artifacts (partial example)</a>
 */
public data class Artifact(
  val id: ComponentArtifactIdentifier,
  val variant: ResolvedVariantResult,
  val file: File,
) {

  internal companion object {
    fun ids(artifacts: Provider<Set<ResolvedArtifactResult>>): Provider<List<ComponentArtifactIdentifier>> {
      return artifacts.map { it.map { artifact -> artifact.id } }
    }

    fun variants(artifacts: Provider<Set<ResolvedArtifactResult>>): Provider<List<ResolvedVariantResult>> {
      return artifacts.map { it.map { artifact -> artifact.variant } }
    }

    fun files(artifacts: Provider<Set<ResolvedArtifactResult>>): Provider<List<File>> {
      return artifacts.map { it.map { artifact -> artifact.file } }
    }

    fun sequenced(
      ids: Provider<List<ComponentArtifactIdentifier>>,
      variants: Provider<List<ResolvedVariantResult>>,
      files: Provider<List<File>>,
    ): Sequence<Artifact> = zipped(ids, variants, files).asSequence()

    fun zipped(
      ids: Provider<List<ComponentArtifactIdentifier>>,
      variants: Provider<List<ResolvedVariantResult>>,
      files: Provider<List<File>>,
    ): List<Artifact> {
      // If all are missing, that's fine, we assume this is an @Optional situation
      if (!ids.isPresent && !variants.isPresent && !files.isPresent) {
        return emptyList()
      }
      // If only one is missing, that's an error
      require(ids.isPresent && variants.isPresent && files.isPresent) {
        "Expected both 'ids', 'variants', and 'files' to be present. Got ids=${ids.isAvailable()}, variants=${variants.isAvailable()}, and files=${files.isAvailable()}"
      }

      // All are present and must be the same length, or it's an error
      val ids = ids.get()
      val variants = variants.get()
      val files = files.get()
      require(ids.size == files.size && variants.size == files.size) {
        "Expected 'details.size == variants.size == files.size'. Got ids.size=${ids.size}, variants.size=${variants.size}, files.size=${files.size}"
      }

      return ids.zip(variants).zip(files).map { (details, file) ->
        Artifact(
          id = details.first,
          variant = details.second,
          file = file,
        )
      }
    }

    private fun Provider<*>.isAvailable(): String = if (isPresent) "available" else "not available"
  }
}
