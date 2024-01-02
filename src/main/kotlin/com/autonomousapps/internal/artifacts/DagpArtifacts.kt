// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

internal interface DagpArtifacts : Named {
  companion object {
    @JvmField val DAGP_ARTIFACTS_ATTRIBUTE: Attribute<DagpArtifacts> = Attribute.of(
      "dagp.internal.artifacts", DagpArtifacts::class.java
    )
  }

  enum class Kind(
    val declarableName: String,
    val artifactName: String,
  ) {
    PROJECT_HEALTH("projectHealth", "project-health"),
    RESOLVED_DEPS("resolvedDeps", "resolved-deps"),
  }
}
