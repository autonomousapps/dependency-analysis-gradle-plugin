// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/** Teach Gradle how custom configurations relate to each other, and the artifacts they provide and consume. */
internal class Attr<T : Named>(
  val attribute: Attribute<T>,
  val attributeName: String,
)
