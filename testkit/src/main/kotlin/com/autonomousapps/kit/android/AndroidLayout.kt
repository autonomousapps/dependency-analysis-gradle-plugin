package com.autonomousapps.kit.android

public class AndroidLayout(
  public val filename: String,
  public val content: String,
) {
  override fun toString(): String = content
}
