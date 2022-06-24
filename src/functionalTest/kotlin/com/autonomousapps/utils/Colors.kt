package com.autonomousapps.utils

// TODO: move into common test src folder
object Colors {

  private val colorRegex = """\u001B\[.+?m""".toRegex()

  @JvmStatic
  fun String.decolorize(): String = replace(colorRegex, "")
}
