// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.android

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import java.lang.reflect.Method

internal abstract class BaseAndroidGradlePlugin(
  protected val project: Project,
  protected val agpVersion: String
) : AndroidGradlePlugin {

  protected abstract val bundleTaskType: String
  protected abstract val bundleTaskOutputMethodName: String

  @Suppress("UNCHECKED_CAST")
  protected fun getBundleTaskType(): Class<out Task> = try {
    Class.forName(bundleTaskType) as Class<Task>
  } catch (e: ClassNotFoundException) {
    throw GradleException("Cannot find bundle class for AGP $agpVersion")
  }

  protected fun getOutputMethod(type: Class<out Task>): Method = try {
    type.getDeclaredMethod(bundleTaskOutputMethodName)
  } catch (e: NoSuchMethodException) {
    throw GradleException("Cannot find output method name for AGP $agpVersion")
  }
}
