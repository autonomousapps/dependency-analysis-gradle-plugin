// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.ManifestParser.ManifestParseException
import com.autonomousapps.model.internal.AndroidResSource.AttrRef
import com.google.common.truth.Truth.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ManifestParserTest {

  @TempDir lateinit var tempFolder: Path

  @Test fun `parse package name`() {
    val manifest = parse(
      """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.app" />
      """.trimIndent()
    )

    assertThat(manifest.packageName).isEqualTo("com.app")
  }

  @Test fun `DSL namespace supersedes XML namespace`() {
    val manifest = parse(
      manifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.app" />
        """.trimIndent(),
      dslNamespace = "better.app"
    )

    assertThat(manifest.packageName).isEqualTo("better.app")
  }

  @Test fun `doesn't need package name in XML if there is a DSL namespace`() {
    val manifest = parse(
      manifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" />
        """.trimIndent(),
      dslNamespace = "better.app"
    )

    assertThat(manifest.packageName).isEqualTo("better.app")
  }

  @Test fun `throws ManifestParseException when manifest is missing package declaration and there is no DSL namespace`() {
    assertThrows(ManifestParseException::class.java) {
      parse(
        """
          <?xml version="1.0" encoding="utf-8"?>
          <manifest xmlns:android="http://schemas.android.com/apk/res/android" />
        """.trimIndent()
      )
    }
  }

  @Test fun `parse services`() {
    val manifest = parse(
      """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          package="com.app" >
      
          <application>
              <service
                  android:name="com.app.ServiceA"
                  android:exported="false"/>
              <service
                  android:name=".ServiceB"
                  android:exported="false"/>
          </application>
      </manifest>
      """.trimIndent()
    )

    val services = manifest.components["services"]
    assertThat(services).isNotNull()
    assertThat(services).isEqualTo(
      setOf("com.app.ServiceA", "com.app.ServiceB")
    )
  }

  @Test fun `parse providers`() {
    val manifest = parse(
      """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          package="com.app" >
          
          <queries>
          
            <package android:name="com.otherapp" />
            
            <intent>
                <action android:name="com.otherapp.ACTION" />
            </intent>
            
            <provider
              android:authorities="com.otherapp.provider" />
              
          </queries>
      
          <application>
              <provider
                  android:name="com.app.ContentProviderA"
                  android:authorities="com.app.provider"
                  android:enabled="true"
                  android:exported="true" >
              </provider>
              <provider
                  android:name=".ContentProviderB"
                  android:authorities="com.app.provider"
                  android:enabled="true"
                  android:exported="true" >
              </provider>
          </application>
      </manifest>
      """.trimIndent()
    )

    val providers = manifest.components["providers"]
    assertThat(providers).isNotNull()
    assertThat(providers).isEqualTo(
      setOf("com.app.ContentProviderA", "com.app.ContentProviderB")
    )
  }

  @Test fun `parse application name`() {
    val manifest = parse(
      """
      <?xml version="1.0" encoding="utf-8"?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
          package="com.app" >
      
          <application android:name="mutual.aid.explode"/>
      </manifest>
      """.trimIndent()
    )

    assertThat(manifest.applicationName).isEqualTo("mutual.aid.explode")
  }

  @Test fun `parse resource references`() {
    val manifest = parse(
      manifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:tools="http://schemas.android.com/tools">
        
            <application android:icon="@mipmap/ic_launcher" android:label="@string/app_name"
                android:networkSecurityConfig="@xml/network_security_config"
                android:theme="@style/TheEternalVoid">
                <activity android:name=".MainActivity" android:theme="@style/TheTwistedLand">
                    <intent-filter android:autoVerify="true">
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <data android:scheme="https" />
                        <data android:host="@string/deeplink_host" />
                        <data android:path="@string/deeplink_path" />
                    </intent-filter>
                </activity>
                <provider android:name="androidx.startup.InitializationProvider"
                    android:authorities="${'$'}{applicationId}.androidx-startup"
                    android:exported="false" tools:node="merge">
                    <meta-data android:name="com.app.MyInitializer"
                        android:value="@string/androidx_startup" />
                </provider>
                <meta-data android:name="com.google.android.geo.API_KEY"
                    android:value="@string/google_maps_api_key" />
                <meta-data android:name="com.google.android.gms.version"
                    android:value="@integer/google_play_services_version" />
                <meta-data android:name="google_analytics_default_allow_analytics_storage"
                    android:value="@bool/google_analytics_enabled" />
            </application>
        </manifest>
      """.trimIndent(),
      dslNamespace = "com.app"
    )

    assertThat(manifest.attrRefs).containsExactly(
      AttrRef(type = "mipmap", id = "ic_launcher"),
      AttrRef(type = "string", id = "app_name"),
      AttrRef(type = "xml", id = "network_security_config"),
      AttrRef(type = "style", id = "TheEternalVoid"),
      AttrRef(type = "style", id = "TheTwistedLand"),
      AttrRef(type = "string", id = "deeplink_host"),
      AttrRef(type = "string", id = "deeplink_path"),
      AttrRef(type = "string", id = "androidx_startup"),
      AttrRef(type = "string", id = "google_maps_api_key"),
      AttrRef(type = "integer", id = "google_play_services_version"),
      AttrRef(type = "bool", id = "google_analytics_enabled"),
    )
  }

  private fun parse(@Language("XML") manifest: String, dslNamespace: String = ""): ManifestParser.ParseResult {
    val file = tempFolder.resolve("AndroidManifest.xml").toFile()
    file.writeText(manifest)
    return ManifestParser(dslNamespace).parse(file)
  }
}
