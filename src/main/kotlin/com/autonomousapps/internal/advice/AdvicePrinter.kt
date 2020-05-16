package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.ConsoleReport
import org.gradle.api.GradleException

/**
 * Only concerned with human-readable advice meant to be printed to the console.
 */
internal class AdvicePrinter(
  private val consoleReport: ConsoleReport,
  private val dependencyRenamingMap: Map<String, String>? = null
) {
  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  fun getAddAdvice(): String? {
    val undeclaredApiDeps = consoleReport.addToApiAdvice
    val undeclaredImplDeps = consoleReport.addToImplAdvice

    if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) {
      return null
    }

    val apiAdvice = undeclaredApiDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it)})"
    }
    val implAdvice = undeclaredImplDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it)})"
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
  fun getRemoveAdvice(): String? {
    val unusedDependencies = consoleReport.removeAdvice

    if (unusedDependencies.isEmpty()) {
      return null
    }

    return unusedDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.configurationName}(${printableIdentifier(it)})"
    }
  }

  /**
   * Returns "change-advice" (or null if none) for printing to console.
   */
  fun getChangeAdvice(): String? {
    val changeToApi = consoleReport.changeToApiAdvice
    val changeToImpl = consoleReport.changeToImplAdvice

    if (changeToApi.isEmpty() && changeToImpl.isEmpty()) {
      return null
    }

    val apiAdvice = changeToApi.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it)}) (was ${it.configurationName})"
    }
    val implAdvice = changeToImpl.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it)}) (was ${it.configurationName})"
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
  fun getCompileOnlyAdvice(): String? {
    val compileOnlyDependencies = consoleReport.compileOnlyDependencies

    if (compileOnlyDependencies.isEmpty()) {
      return null
    }

    return compileOnlyDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it)}) (was ${it.configurationName})"
    }
  }

  fun getRemoveProcAdvice(): String? {
    val unusedProcs = consoleReport.unusedProcsAdvice

    if (unusedProcs.isEmpty()) {
      return null
    }

    return unusedProcs.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.configurationName}(${printableIdentifier(it)})"
    }
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      val dependencyId = "${dependency.identifier}:${dependency.resolvedVersion}"
      dependencyRenamingMap?.getOrDefault(dependencyId, null) ?: "\"$dependencyId\""
    }
}