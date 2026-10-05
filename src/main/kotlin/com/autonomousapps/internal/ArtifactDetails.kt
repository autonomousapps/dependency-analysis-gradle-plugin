// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.tasks.Input

/** Only public because it's used as a task input. Must be used with `@Nested`. */
public data class ArtifactDetails(
  @get:Input val id: ComponentArtifactIdentifier,
  @get:Input val variant: ResolvedVariantResult,
)
