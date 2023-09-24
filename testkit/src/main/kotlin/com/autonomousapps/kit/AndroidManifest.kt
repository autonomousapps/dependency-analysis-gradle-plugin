package com.autonomousapps.kit

class AndroidManifest(val content: String) {

  override fun toString(): String = content

  companion object {

    private const val DEFAULT_APP_PACKAGE_NAME = "com.example"

    @JvmStatic
    fun of(content: String) = AndroidManifest(content)

    @JvmStatic
    fun simpleApp(): AndroidManifest = AndroidManifest(
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
    fun app(
      application: String? = null,
      activities: List<String> = emptyList()
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
    fun appWithoutPackage(application: String? = null): AndroidManifest {
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
    fun app(application: String? = null): AndroidManifest {
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
    val DEFAULT_APP = app(null)

    @JvmStatic
    fun defaultLib(packageName: String) = AndroidManifest(
      """
      |<?xml version="1.0" encoding="utf-8"?>
      |<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      |  package="$packageName"/>
      """.trimMargin()
    )
  }
}
