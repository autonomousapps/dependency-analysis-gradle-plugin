package com.autonomousapps.kit

final class AndroidManifest {

  final String content

  AndroidManifest(String content) {
    this.content = content
  }

  @Override
  String toString() {
    return content
  }

  static final AndroidManifest DEFAULT_MANIFEST = new AndroidManifest(
    """\
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.example">
      
      <application
        android:allowBackup="true"
        android:label="Test app"
        android:theme="@style/AppTheme"
        >
        <activity
          android:name=".MainActivity"
          android:label="MainActivity"
          android:theme="@style/AppTheme.NoActionBar">
            <intent-filter>
              <action android:name="android.intent.action.MAIN" />
              <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
          </activity>
        </application>
      </manifest>
    """.stripIndent()
  )
}
