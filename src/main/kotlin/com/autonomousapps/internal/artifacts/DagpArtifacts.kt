package com.autonomousapps.internal.artifacts

import com.autonomousapps.artifacts.ArtifactDescription
import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

internal interface DagpArtifacts : Named {

  companion object {
    @JvmField
    val DAGP_ARTIFACTS_ATTRIBUTE: Attribute<DagpArtifacts> =
      Attribute.of("dagp.internal.artifacts", DagpArtifacts::class.java)
  }

  enum class Kind : ArtifactDescription<DagpArtifacts> {
    COMBINED_GRAPH,
    PROJECT_HEALTH,
    RESOLVED_DEPS,
    ;

    override val attribute: Attribute<DagpArtifacts> = DAGP_ARTIFACTS_ATTRIBUTE

    override val categoryName: String = "dependency-analysis"
  }
}
