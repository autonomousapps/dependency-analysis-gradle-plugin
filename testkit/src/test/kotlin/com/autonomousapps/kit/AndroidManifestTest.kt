package com.autonomousapps.kit

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class AndroidManifestTest {

  @Test fun `multiline strings are pretty`() {
    val manifest = AndroidManifest.app(activities = listOf("MainActivity"))
    println(manifest)

    assertThat(manifest.toString()).isEqualTo(
      """
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
          >
          <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
          </intent-filter>
        </activity>
        </application>
      </manifest>
      """.trimIndent()
    )
  }
}
