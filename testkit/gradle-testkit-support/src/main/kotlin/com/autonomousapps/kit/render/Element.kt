package com.autonomousapps.kit.render

public sealed interface Element {

  public fun render(scribe: Scribe): String

  public fun start(indent: Int): String = " ".repeat(indent)

  public interface Block : Element {
    public val name: String
  }

  public interface Line : Element
}
