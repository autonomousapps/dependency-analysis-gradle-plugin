// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.internal.utils.document.attrs
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class XmlParserTest {

  @TempDir lateinit var tempDir: Path

  @Test fun `can read vector drawable`() {
    // given
    val vectorDrawable = """
      <?xml version="1.0" encoding="utf-8"?>
      <vector xmlns:android="http://schemas.android.com/apk/res/android"
          android:width="36dp"
          android:height="36dp"
          android:viewportWidth="36"
          android:viewportHeight="36">

          <!-- This usage is not detected -->
          <path
              android:fillColor="?themeColor"
              android:pathData="M0.000418269 15C0.0223146 17.9111 0.904212 20.846 2.71627 23.4108L12.9056 37.9142C13.9228 39.362 16.0781 39.3619 17.0952 37.9141L27.2873 23.4053C29.0977 20.8428 29.9786 17.9098 30.0002 15C30 6.71573 23.2843 0 15 0C6.71573 0 0 6.71573 0 15" />
      </vector>
    """.trimIndent()
    val path = tempDir.resolve("ic_pin.xml").apply {
      writeText(vectorDrawable)
    }

    // when
    val attrs = buildDocument(path).attrs()

    // then
    val fillColor = attrs.find { it.first == "android:fillColor" }
    assertThat(fillColor).isNotNull()
    assertThat(fillColor!!.second).isEqualTo("?themeColor")
  }

  private fun Path.writeText(text: String) {
    Files.write(this, text.toByteArray())
  }
}
