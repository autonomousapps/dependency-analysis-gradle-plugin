@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal

import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider

/*
 * These utilities are for interacting with AGP. Particularly for using different versions of AGP.
 */

/**
 * Returns a reference to the [BundleLibraryClasses] task associated with the given [Project]. Uses [agpVersion] to
 * get the correct name, based on AGP version, as well as get a reference to the correct [RegularFileProperty] output
 * for the task. This latter uses reflection.
 */
fun getBundleTaskOutput(project: Project, agpVersion: String, variantName: String): Provider<RegularFile> {
    val bundleTaskName = getBundleTaskName(agpVersion, variantName)
    val task = project.tasks.named(bundleTaskName, BundleLibraryClasses::class.java)

    val outputMethod = try {
        BundleLibraryClasses::class.java.getDeclaredMethod(getOutputPropertyName(agpVersion))
    } catch (e: NoSuchMethodException) {
        throw GradleException("Cannot find output method name for AGP $agpVersion")
    }

    return task.flatMap {
        outputMethod.invoke(it) as RegularFileProperty
    }
}

private fun getBundleTaskName(agpVersion: String, variantName: String) = when {
    // Handle newer versions when they are released
    agpVersion == "4.0.0-alpha09" -> "bundleLibCompileToJar$variantName"
    agpVersion.startsWith("4.0.0-alpha0") -> "bundleLibCompile$variantName"
    agpVersion.startsWith("3.6.") -> "bundleLibCompile$variantName"
    agpVersion.startsWith("3.5.") -> "bundleLibCompile$variantName"
    else -> "bundleLibCompile$variantName"
}

private fun getOutputPropertyName(agpVersion: String) = when {
    // Handle newer versions when they are released
    agpVersion == "4.0.0-alpha09" -> "getJarOutput"
    agpVersion.startsWith("4.0.0-alpha0") -> "getOutput"
    agpVersion.startsWith("3.6.") -> "getOutput"
    agpVersion.startsWith("3.5.") -> "getOutput"
    else -> "getOutput"
}
