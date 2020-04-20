package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency
import org.gradle.api.GradleException
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.lang.StringBuilder

/**
 * Only concerned with human-readable advice meant to be printed to the console.
 */
internal class AdvicePrinter(private val computedAdvice: ComputedAdvice) {

  data class ConsoleReport(private val report: StringBuilder, val hasAdvices: Boolean) {
    override fun toString() = report.toString()
  }
  
  fun reportAdviceToConsole(): ConsoleReport {
    var didGiveAdvice = false
    val consoleReport = StringBuilder()

    getRemoveAdvice()?.let {
      consoleReport.append("Unused dependencies which should be removed:\n$it\n")
      didGiveAdvice = true
    }

    getAddAdvice()?.let {
      consoleReport.append("Transitively used dependencies that should " +
        "be declared directly as indicated:\n$it\n")
      didGiveAdvice = true
    }

    getChangeAdvice()?.let {
      consoleReport.append("Existing dependencies which should be modified " +
        "to be as indicated:\n$it\n")
      didGiveAdvice = true
    }

    getCompileOnlyAdvice()?.let {
      consoleReport.append("Dependencies which could be compile-only:\n$it\n")
      didGiveAdvice = true
    }

    getRemoveProcAdvice()?.let {
      consoleReport.append("Unused annotation processors that should be removed:\n$it\n")
      didGiveAdvice = true
    }

    if (!didGiveAdvice) {
      consoleReport.append("Looking good! No changes needed")
    }
    return ConsoleReport(consoleReport, didGiveAdvice)
  }

  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  @TestOnly fun getAddAdvice(): String? {
    if(computedAdvice.filterAdd) {
      return null
    }
    
    val undeclaredApiDeps = computedAdvice.addToApiAdvice
    val undeclaredImplDeps = computedAdvice.addToImplAdvice

    if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) {
      return null
    }

    val apiAdvice = undeclaredApiDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it.dependency)})"
    }
    val implAdvice = undeclaredImplDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it.dependency)})"
    }

    return if (undeclaredApiDeps.isNotEmpty() && undeclaredImplDeps.isNotEmpty()) {
      "$apiAdvice\n$implAdvice"
    } else if (undeclaredApiDeps.isNotEmpty()) {
      apiAdvice
    } else if (undeclaredImplDeps.isNotEmpty()) {
      implAdvice
    } else {
      // One or the other list must be non-empty
      throw GradleException("Impossible")
    }
  }

  /**
   * Returns "remove-advice" (or null if none) for printing to console.
   */
  @TestOnly fun getRemoveAdvice(): String? {
    if(computedAdvice.filterRemove) {
      return null
    }
    
    val unusedDependencies = computedAdvice.removeAdvice

    if (unusedDependencies.isEmpty()) {
      return null
    }

    return unusedDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }
  }

  /**
   * Returns "change-advice" (or null if none) for printing to console.
   */

  @TestOnly fun getChangeAdvice(): String? {
    if(computedAdvice.filterChange) {
      return null
    }
    
    val changeToApi = computedAdvice.changeToApiAdvice
    val changeToImpl = computedAdvice.changeToImplAdvice

    if (changeToApi.isEmpty() && changeToImpl.isEmpty()) {
      return null
    }

    val apiAdvice = changeToApi.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }
    val implAdvice = changeToImpl.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }
    return if (changeToApi.isNotEmpty() && changeToImpl.isNotEmpty()) {
      "$apiAdvice\n$implAdvice"
    } else if (changeToApi.isNotEmpty()) {
      apiAdvice
    } else if (changeToImpl.isNotEmpty()) {
      implAdvice
    } else {
      // One or the other list must be non-empty
      throw GradleException("Impossible")
    }
  }

  /**
   * Returns "compileOnly-advice" (or null if none) for printing to console.
   */
  private fun getCompileOnlyAdvice(): String? {
    if (computedAdvice.filterCompileOnly) {
      return null
    }
    
    val compileOnlyDependencies = computedAdvice.compileOnlyDependencies

    if (compileOnlyDependencies.isEmpty()) {
      return null
    }

    return compileOnlyDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it)}) (was ${it.configurationName})"
    }
  }

  private fun getRemoveProcAdvice(): String? {
    // TODO add filter
    val unusedProcs = computedAdvice.unusedProcsAdvice

    if (unusedProcs.isEmpty()) {
      return null
    }

    return unusedProcs.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.dependency.configurationName}(${printableIdentifier(it.dependency)})"
    }
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      "\"${dependency.identifier}:${dependency.resolvedVersion}\""
    }
}