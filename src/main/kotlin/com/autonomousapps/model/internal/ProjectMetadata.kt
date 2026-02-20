// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.model.internal.ProjectType
import com.squareup.moshi.JsonClass

/**
 * Metadata about the project. Public because it's a task input, but should be considered an internal implementation
 * detail.
 */
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
