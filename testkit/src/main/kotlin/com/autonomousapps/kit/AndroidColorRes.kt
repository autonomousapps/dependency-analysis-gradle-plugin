package com.autonomousapps.kit

class AndroidColorRes(
  val colors: List<AndroidColor>
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

    val DEFAULT = AndroidColorRes(DEFAULT_COLORS)
  }

  class AndroidColor(val name: String, val value: String) {
    override fun toString(): String {
      return "<color name=\"$name\">$value</color>"
    }
  }
}
