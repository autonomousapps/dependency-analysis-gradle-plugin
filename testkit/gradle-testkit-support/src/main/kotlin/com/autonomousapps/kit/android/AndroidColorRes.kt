// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.android

public class AndroidColorRes @JvmOverloads constructor(
  private val colors: List<AndroidColor> = emptyList(),
) {

  override fun toString(): String {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  ${colors.joinToString("\n  ")}\n</resources>"
  }

  internal fun isBlank(): Boolean = colors.isEmpty() || colors.all { it.name.isBlank() && it.value.isBlank() }

  public companion object {
    private val DEFAULT_COLORS = listOf(
      AndroidColor("colorPrimaryDark", "#0568ae"),
      AndroidColor("colorPrimary", "#009fdb"),
      AndroidColor("colorAccent", "#009fdb")
    )

    @JvmField
    public val DEFAULT: AndroidColorRes = AndroidColorRes(DEFAULT_COLORS)

    @JvmField
    public val EMPTY: AndroidColorRes = AndroidColorRes()
  }

  public class AndroidColor(
    public val name: String,
    public val value: String,
  ) {
    override fun toString(): String {
      return "<color name=\"$name\">$value</color>"
    }
  }
}
