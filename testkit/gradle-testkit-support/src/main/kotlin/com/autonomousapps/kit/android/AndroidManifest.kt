// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.android

public class AndroidManifest(public val content: String) {

  override fun toString(): String = content

  public companion object {

    @JvmStatic
    public fun of(content: String): AndroidManifest = AndroidManifest(content)

    @JvmStatic
    public fun simpleApp(): AndroidManifest = AndroidManifest(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      |  package="com.example">
      |
      |<application
      |  android:allowBackup="false"
      |  android:label="Test app">
      |</application>
      |</manifest>
      """.trimMargin()
    )

    @JvmStatic
    public fun app(
      application: String? = null,
      activities: List<String> = emptyList(),
    ): AndroidManifest = AndroidManifest(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      |  package="com.example">
      |
      |<application
      |  android:allowBackup="true"
      |  android:label="Test app"
      |  android:theme="@style/AppTheme"
      |  ${application?.let { "android:name=\"$it\"" } ?: ""}>
      |  ${activities.joinToString(separator = "\n") { activityBlock(it) }}
      |  </application>
      |</manifest>
      """.trimMargin()
    )

    @JvmStatic
    public fun appWithoutPackage(application: String? = null): AndroidManifest {
      return AndroidManifest(
        """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
        |
        |<application
        |  android:allowBackup="true"
        |  android:label="Test app"
        |  android:theme="@style/AppTheme"
        |  ${application?.let { "android:name=\"$it\"" } ?: ""}>
        |  ${activityBlock()}
        |  </application>
        |</manifest>
        """.trimMargin()
      )
    }

    @JvmStatic
    public fun app(application: String? = null): AndroidManifest {
      return AndroidManifest(
        """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        |  package="com.example">
        |
        |<application
        |  android:allowBackup="true"
        |  android:label="Test app"
        |  android:theme="@style/AppTheme"
        |  ${application?.let { "android:name=\"$it\"" } ?: ""}>
        |  ${activityBlock()}
        |  </application>
        |</manifest>
        """.trimMargin()
      )
    }

    private fun activityBlock(activityName: String = "MainActivity"): String =
      """
      |  <activity
      |    android:name=".$activityName"
      |    android:label="$activityName"
      |    >
      |    <intent-filter>
      |      <action android:name="android.intent.action.MAIN" />
      |      <category android:name="android.intent.category.LAUNCHER" />
      |    </intent-filter>
      |  </activity>"""

    @JvmField
    public val DEFAULT_APP: AndroidManifest = app(null)

    // TODO: stop using package
    @JvmStatic
    public fun defaultLib(packageName: String): AndroidManifest = AndroidManifest(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      |  package="$packageName"/>
      """.trimMargin()
    )
  }
}
