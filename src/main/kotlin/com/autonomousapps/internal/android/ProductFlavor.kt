// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.android

import java.io.Serializable

/** Only public because it's a task input. */
public data class ProductFlavor(
  val dimension: String,
  val flavorName: String,
) : Serializable
