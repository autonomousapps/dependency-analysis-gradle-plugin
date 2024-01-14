// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.internal

internal fun String.ensurePrefix(prefix: String = ":"): String {
  return if (startsWith(prefix)) this else "$prefix$this"
}
