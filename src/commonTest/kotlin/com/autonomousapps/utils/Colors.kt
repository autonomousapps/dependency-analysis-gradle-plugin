// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.utils

object Colors {

  private val colorRegex = """\u001B\[.+?m""".toRegex()

  @JvmStatic
  fun String.decolorize(): String = replace(colorRegex, "")
}
