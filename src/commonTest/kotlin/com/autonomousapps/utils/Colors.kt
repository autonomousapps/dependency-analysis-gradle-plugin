package com.autonomousapps.utils

object Colors {

  private val colorRegex = """\u001B\[.+?m""".toRegex()

  @JvmStatic
  fun String.decolorize(): String = replace(colorRegex, "")
}
