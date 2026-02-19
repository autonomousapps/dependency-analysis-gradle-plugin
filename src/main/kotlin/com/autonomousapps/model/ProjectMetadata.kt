// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.ProjectType
import com.squareup.moshi.JsonClass

/** Metadata about the project. */
@JsonClass(generateAdapter = false)
public data class ProjectMetadata(
  public val projectPath: String,
  public val projectType: ProjectType,
) : Comparable<ProjectMetadata> {
  override fun compareTo(other: ProjectMetadata): Int {
    return compareBy<ProjectMetadata>(ProjectMetadata::projectPath)
      .thenBy(ProjectMetadata::projectType)
      .compare(this, other)
  }
}
