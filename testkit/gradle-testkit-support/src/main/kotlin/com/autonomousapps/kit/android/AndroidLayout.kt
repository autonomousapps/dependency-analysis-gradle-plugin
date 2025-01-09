// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.android

public class AndroidLayout(
  public val filename: String,
  public val content: String,
) {
  override fun toString(): String = content
}
