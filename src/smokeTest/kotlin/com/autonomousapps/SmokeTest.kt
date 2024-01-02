// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("FunctionName")

package com.autonomousapps

import com.autonomousapps.fixtures.newSimpleProject
import com.google.common.truth.Truth.assertThat
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logging
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

const val WORKSPACE = "build/smokeTest"

class SmokeTest {

  private val logger = Logging.getLogger(SmokeTest::class.java)

  private lateinit var theProjectDir: File
  private val projectVersion = System.getProperty("com.autonomousapps.version").also {
    logger.quiet("Testing version $it")
  }

  @AfterEach fun cleanup() {
    FileUtils.deleteDirectory(theProjectDir)
  }

  // This will catch the case that led to this commit: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/commit/ffe4d30302a73acab2dfd259b20998f3604b5963
  // Had to revert modularization because Gradle couldn't resolve project dependency. Tests passed locally, but plugin
  // could not be used by normal consumers.
  @Test fun `binary plugin can be applied`() {
    theProjectDir = newSimpleProject(projectVersion)

    val result = GradleRunner.create().apply {
      forwardOutput()
      withPluginClasspath()
      withGradleVersion(GradleVersion.current().version)
      withProjectDir(theProjectDir)
      withArguments("help")
    }.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
