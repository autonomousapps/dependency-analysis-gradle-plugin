// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal interface AndroidVariant {
  val flavorName: String
  val variantName: String
  val buildType: String
  val testInstrumentationRunner: Provider<String?>
}

internal class DefaultAndroidVariant(
  override val flavorName: String,
  override val variantName: String,
  override val buildType: String,
  override val testInstrumentationRunner: Provider<String?>,
) : AndroidVariant {
  constructor(project: Project, variant: Variant) : this(
    flavorName = variant.flavorName.orEmpty(),
    variantName = variant.name,
    buildType = variant.buildType.orEmpty(),
    testInstrumentationRunner = getTestInstrumentationRunner(project, variant),
  )

  private companion object {
    fun getTestInstrumentationRunner(project: Project, variant: Variant): Provider<String?> {
      return if (variant is HasAndroidTest) {
        variant.androidTest?.instrumentationRunner ?: project.provider { null }
      } else {
        project.provider { null }
      }
    }
  }
}
