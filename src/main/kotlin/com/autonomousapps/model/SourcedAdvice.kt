// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass


/**
 * Wrapper for advice class with source data attached
 * (e.g. in which line of the build file does the source of this advice originate)
 */
@JsonClass(generateAdapter = false)
public data class SourcedAdvice(
  val advice: Advice,
  val buildFileDeclarationLineNumber: Int? = null,
)
