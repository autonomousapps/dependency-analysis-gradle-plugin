package com.autonomousapps.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManifestParserTest {

  @get:Rule val tempFolder = TemporaryFolder()

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

  private fun parse(manifest: String): ManifestParser.ParseResult {
    val file = tempFolder.newFile("AndroidManifest.xml")
    file.writeText(manifest)
    return ManifestParser().parse(file)
  }
}