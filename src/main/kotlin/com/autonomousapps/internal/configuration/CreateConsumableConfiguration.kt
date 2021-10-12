package com.autonomousapps.internal.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

internal fun Project.createConsumableConfiguration(confName: String): Configuration =
  configurations.create(confName) {
    isCanBeResolved = true
    isCanBeConsumed = false
  }