// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
public data class SourcedProjectAdvice(
  val projectPath: String,
  val dependencyAdvice: Set<SourcedAdvice> = emptySet(),
  val pluginAdvice: Set<PluginAdvice> = emptySet(),
  val moduleAdvice: Set<ModuleAdvice> = emptySet(),
  val warning: Warning = Warning.empty(),
  /** True if there is any advice in a category for which the user has declared they want the build to fail. */
  val shouldFail: Boolean = false,
  val projectBuildFile: String? = null,
)
