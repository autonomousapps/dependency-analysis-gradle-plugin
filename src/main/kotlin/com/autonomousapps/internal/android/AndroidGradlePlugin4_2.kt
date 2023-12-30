// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ClassName", "UnstableApiUsage")

package com.autonomousapps.internal.android

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider

internal class AndroidGradlePlugin4_2(
  project: Project,
  agpVersion: String,
) : BaseAndroidGradlePlugin(project, agpVersion) {

  override val bundleTaskType: String = "com.android.build.gradle.internal.tasks.BundleLibraryClassesJar"
  override val bundleTaskOutputMethodName: String = "getOutput"

  override fun getBundleTaskOutput(variantName: String): Provider<RegularFile> {
    val bundleTaskName = "bundleLibCompileToJar$variantName"
    val type = getBundleTaskType()
    val task = project.tasks.named(bundleTaskName, type)
    val outputMethod = getOutputMethod(type)

    return task.flatMap {
      outputMethod.invoke(it) as RegularFileProperty
    }
  }

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
