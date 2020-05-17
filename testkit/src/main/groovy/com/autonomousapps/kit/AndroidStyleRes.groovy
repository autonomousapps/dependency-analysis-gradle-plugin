package com.autonomousapps.kit

final class AndroidStyleRes {

  final String content

  AndroidStyleRes(String content) {
    this.content = content
  }

  @Override
  String toString() {
    return content
  }

  static final DEFAULT = new AndroidStyleRes(
    """\
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
    """.stripIndent()
  )
}
