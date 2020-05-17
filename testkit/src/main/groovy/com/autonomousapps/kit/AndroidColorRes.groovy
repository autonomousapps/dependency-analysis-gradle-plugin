package com.autonomousapps.kit

final class AndroidColorRes {

  final List<AndroidColor> colors

  AndroidColorRes(List<AndroidColor> colors) {
    this.colors = colors
  }

  @Override
  String toString() {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  ${colors.join('\n  ')}\n</resources>"
  }

  static final List<AndroidColor> DEFAULT_COLORS = [
    new AndroidColor("colorPrimaryDark", "#0568ae"),
    new AndroidColor("colorPrimary", "#009fdb"),
    new AndroidColor("colorAccent", "#009fdb")
  ]

  static final AndroidColorRes DEFAULT_COLORS_XML = new AndroidColorRes(DEFAULT_COLORS)

  static final class AndroidColor {

    final String name, value

    AndroidColor(String name, String value) {
      this.name = name
      this.value = value
    }

    @Override
    String toString() {
      return "<color name=\"$name\">$value</color>"
    }
  }
}
