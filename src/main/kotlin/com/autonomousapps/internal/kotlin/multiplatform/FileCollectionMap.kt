// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** This type is public because it's a task input, but should be considered an internal implementation detail. */
public data class FileCollectionMap(
  /** E.g., `commonMain`, `commonTest`. */
  @Input val name: String,

  /** The [org.gradle.api.file.FileCollection] for the source set named [name]. Includes generated source. */
  @get:PathSensitive(PathSensitivity.RELATIVE) @InputFiles val files: FileCollection,
)
