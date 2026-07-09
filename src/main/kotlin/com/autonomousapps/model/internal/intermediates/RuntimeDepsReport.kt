// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class RuntimeDepsReport(
  val projectPath: String,
  /** Fully-qualified class names referenced in runtime configs (Spring @Bean, @Import, Liquibase) */
  val runtimeReferencedClasses: Set<String>,
  /** Package prefixes from @ComponentScan */
  val componentScanPackages: Set<String>,
)
