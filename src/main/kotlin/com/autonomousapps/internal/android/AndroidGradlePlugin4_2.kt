// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ClassName", "UnstableApiUsage")

package com.autonomousapps.internal.android

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal class AndroidGradlePlugin4_2(private val project: Project) : AndroidGradlePlugin {

  override fun isViewBindingEnabled(): Provider<Boolean> {
    return project.provider { project.extensions.getByType(CommonExtension::class.java).viewBinding.enable }
  }

  override fun isDataBindingEnabled(): Provider<Boolean> {
    return project.provider { project.extensions.getByType(CommonExtension::class.java).dataBinding.enable }
  }

  override fun namespace(): Provider<String> {
    return project.providers.provider {
      project.extensions.getByType(CommonExtension::class.java).namespace ?: ""
    }
  }
}
