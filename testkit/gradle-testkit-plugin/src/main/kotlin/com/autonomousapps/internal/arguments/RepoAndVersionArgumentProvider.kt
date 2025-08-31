// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.arguments

import org.gradle.process.CommandLineArgumentProvider

internal class RepoAndVersionArgumentProvider(
  private val repo: String,
  private val version: String,
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> {
    return listOf(
      "-Dcom.autonomousapps.plugin-under-test.repo=$repo",
      "-Dcom.autonomousapps.plugin-under-test.version=$version",
    )
  }
}
