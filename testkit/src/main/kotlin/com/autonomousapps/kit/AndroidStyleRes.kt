package com.autonomousapps.kit

class AndroidStyleRes(val content: String) {

  override fun toString(): String = content

  companion object {

    @JvmStatic
    fun of(content: String) = AndroidStyleRes(content)

    @JvmStatic
    val EMPTY = AndroidStyleRes(
      """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
        </resources>
      """.trimIndent()
    )

    @JvmStatic
    val DEFAULT = AndroidStyleRes(
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
