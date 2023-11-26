package com.autonomousapps.kit.android

public class AndroidStyleRes(public val content: String) {

  override fun toString(): String = content

  internal fun isBlank(): Boolean = content.isBlank() || this == EMPTY

  public companion object {

    @JvmStatic
    public fun of(content: String): AndroidStyleRes = AndroidStyleRes(content)

    @JvmStatic
    public val EMPTY: AndroidStyleRes = AndroidStyleRes(
      """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
        </resources>
      """.trimIndent()
    )

    @JvmStatic
    public val DEFAULT: AndroidStyleRes = AndroidStyleRes(
      """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
          <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
            <item name="colorPrimary">@color/colorPrimary</item>
            <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
            <item name="colorAccent">@color/colorAccent</item>
          </style>
              
          <style name="AppTheme.NoActionBar">
            <item name="windowActionBar">false</item>
            <item name="windowNoTitle">true</item>
          </style>
              
          <style name="AppTheme.AppBarOverlay" parent="ThemeOverlay.AppCompat.Dark.ActionBar" />
          <style name="AppTheme.PopupOverlay" parent="ThemeOverlay.AppCompat.Light" />
        </resources>
      """.trimIndent()
    )
  }
}
