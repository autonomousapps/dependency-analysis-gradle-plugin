package com.autonomousapps.kit.android

class AndroidColorRes @JvmOverloads constructor(
  private val colors: List<AndroidColor> = emptyList()
) {

  override fun toString(): String {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  ${colors.joinToString("\n  ")}\n</resources>"
  }

  companion object {
    private val DEFAULT_COLORS = listOf(
      AndroidColor("colorPrimaryDark", "#0568ae"),
      AndroidColor("colorPrimary", "#009fdb"),
      AndroidColor("colorAccent", "#009fdb")
    )

    @JvmField
    val DEFAULT = AndroidColorRes(DEFAULT_COLORS)

    @JvmField
    val EMPTY = AndroidColorRes()
  }

  class AndroidColor(val name: String, val value: String) {
    override fun toString(): String {
      return "<color name=\"$name\">$value</color>"
    }
  }
}
