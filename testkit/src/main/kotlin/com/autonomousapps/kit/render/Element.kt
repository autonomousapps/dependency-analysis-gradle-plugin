package com.autonomousapps.kit.render

sealed interface Element {

  fun render(scribe: Scribe): String

  fun start(indent: Int): String = " ".repeat(indent)

  interface Block : Element {
    val name: String
  }

  interface Line : Element
}
