package com.autonomousapps.kit.android

public class AndroidStringRes(public val content: String) {
  override fun toString(): String = content

  public companion object {
    @JvmField
    public val DEFAULT: AndroidStringRes = AndroidStringRes(
      """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
          <string name ="hello_world">Hello, world</string>
        </resources>
      """.trimIndent()
    )
  }
}
