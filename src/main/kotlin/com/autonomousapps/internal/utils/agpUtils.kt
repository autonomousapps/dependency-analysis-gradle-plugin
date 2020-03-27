@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.lang.reflect.Method

/*
 * These utilities are for interacting with AGP. Particularly for using different versions of AGP.
 */

/**
 * Returns a reference to the `Bundle` task associated with the given [Project]. Uses [agpVersion] to get the correct
 * name, based on AGP version, as well as get a reference to the correct [RegularFileProperty] output for the task.
 * Makes increasingly heavy use of reflection in order to support an array of AGP versions.
 */
internal fun getBundleTaskOutput(project: Project, agpVersion: String, variantName: String): Provider<RegularFile> {
  val bundleTaskName = getBundleTaskName(agpVersion, variantName)
  val type = getBundleTaskType(agpVersion)
  val task = project.tasks.named(bundleTaskName, type)
  val outputMethod = getOutputMethod(type, agpVersion)

  return task.flatMap {
    outputMethod.invoke(it) as RegularFileProperty
  }
}

private fun getOutputMethod(type: Class<out Task>, agpVersion: String): Method = try {
  type.getDeclaredMethod(getOutputPropertyName(agpVersion))
} catch (e: NoSuchMethodException) {
  throw GradleException("Cannot find output method name for AGP $agpVersion")
}

private fun getBundleTaskName(agpVersion: String, variantName: String) = when {
  // Handle newer versions when they are released
  agpVersion.startsWith("4.1") -> "bundleLibCompileToJar$variantName"
  agpVersion.startsWith("4.0") -> "bundleLibCompileToJar$variantName"
  agpVersion.startsWith("3.6") -> "bundleLibCompile$variantName"
  agpVersion.startsWith("3.5") -> "bundleLibCompile$variantName"
  else -> "bundleLibCompile$variantName"
}

@Suppress("UNCHECKED_CAST")
private fun getBundleTaskType(agpVersion: String): Class<out Task> = try {
  when {
    agpVersion.startsWith("4.1") -> Class.forName("com.android.build.gradle.internal.tasks.BundleLibraryClassesJar")
    else -> Class.forName("com.android.build.gradle.internal.tasks.BundleLibraryClasses")
  } as Class<Task>
} catch (e: ClassNotFoundException) {
  throw GradleException("Cannot find bundle class for AGP $agpVersion")
}

private fun getOutputPropertyName(agpVersion: String) = when {
  // Handle newer versions when they are released
  agpVersion.startsWith("4.1") -> "getOutput"
  agpVersion.startsWith("4.0") -> "getJarOutput"
  agpVersion.startsWith("3.6") -> "getOutput"
  agpVersion.startsWith("3.5") -> "getOutput"
  else -> "getOutput"
}

internal fun BaseExtension.isViewBindingEnabled(agpVersion: String): Boolean {
  @Suppress("DEPRECATION")
  return when {
    // no viewBinding pre-3.6
    AgpVersion.version(agpVersion) < AgpVersion.version("3.6") -> false

    // 4.0+ there's a new DSL
    AgpVersion.version(agpVersion) >= AgpVersion.version("4.0") -> withGroovyBuilder {
      getProperty("buildFeatures").withGroovyBuilder { getProperty("viewBinding") } as Boolean? ?: false
    }

    // 3.6 uses this
    else -> viewBinding.isEnabled
  }
}

internal fun BaseExtension.isDataBindingEnabled(agpVersion: String): Boolean {
  @Suppress("DEPRECATION")
  return when {
    // 4.0+ there's a new DSL
    AgpVersion.version(agpVersion) >= AgpVersion.version("4.0") -> withGroovyBuilder {
      getProperty("buildFeatures").withGroovyBuilder { getProperty("dataBinding") } as Boolean? ?: false
    }

    // 3.5 and 3.6 use this
    else -> dataBinding.isEnabled
  }
}
