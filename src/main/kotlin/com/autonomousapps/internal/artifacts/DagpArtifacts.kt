// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.model.ObjectFactory

internal interface DagpArtifacts : Named {
  companion object {
    @JvmField val DAGP_ARTIFACTS_ATTRIBUTE: Attribute<DagpArtifacts> = Attribute.of(
      "dagp.internal.artifacts", DagpArtifacts::class.java
    )

    @JvmField val CATEGORY_ATTRIBUTE: Attribute<Category> = Category.CATEGORY_ATTRIBUTE

    fun category(objects: ObjectFactory): Category {
      return objects.named(Category::class.java, "dependency-analysis")
    }
  }

  enum class Kind(
    val declarableName: String,
    val artifactName: String,
  ) {
    COMBINED_GRAPH("combinedGraph", "combined-graph"),
    PROJECT_HEALTH("projectHealth", "project-health"),
    RESOLVED_DEPS("resolvedDeps", "resolved-deps"),
  }
}
