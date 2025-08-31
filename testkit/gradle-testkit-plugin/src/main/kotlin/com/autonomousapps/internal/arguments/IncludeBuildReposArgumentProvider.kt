// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.arguments

import org.gradle.process.CommandLineArgumentProvider

internal class IncludeBuildReposArgumentProvider(
  private val includedBuildRepos: List<String>,
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> {
    return listOf(
      "-Dcom.autonomousapps.plugin-under-test.repos-included=${includedBuildRepos.joinToString(separator = ",")}",
    )
  }
}
