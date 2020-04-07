@file:Suppress("UnstableApiUsage", "ClassName")

package com.autonomousapps.internal.android

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

class AndroidGradlePlugin3_5(
  project: Project,
  agpVersion: String
) : BaseAndroidGradlePlugin(project, agpVersion) {

  override val bundleTaskType: String = "com.android.build.gradle.internal.tasks.BundleLibraryClasses"
  override val bundleTaskOutputMethodName: String = "getOutput"

  override fun getBundleTaskOutput(variantName: String): Provider<RegularFile> {
    val bundleTaskName = "bundleLibCompile$variantName"
    val type = getBundleTaskType()
    val task = project.tasks.named(bundleTaskName, type)
    val outputMethod = getOutputMethod(type)

    return task.flatMap {
      outputMethod.invoke(it) as RegularFileProperty
    }
  }

  override fun isViewBindingEnabled(): Boolean = false

  override fun isDataBindingEnabled(): Boolean = project.the<BaseExtension>().dataBinding.isEnabled
}
