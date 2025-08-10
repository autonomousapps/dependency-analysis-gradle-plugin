// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.artifacts

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

/** Teach Gradle how custom configurations relate to each other, and the artifacts they provide and consume. */
public class Attr<T : Named>(
  public val attribute: Attribute<T>,
  public val attributeName: String,
)
