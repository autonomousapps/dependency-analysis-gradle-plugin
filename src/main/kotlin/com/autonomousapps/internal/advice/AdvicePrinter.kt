package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.utils.mapToOrderedSet
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

/**
 * Only concerned with human-readable advice meant to be printed to the console.
 */
internal class AdvicePrinter(
  private val consoleReport: ConsoleReport,
  private val dependencyRenamingMap: Map<String, String>? = null
) {

  /**
   * Returns "remove-advice" (or null if none) for printing to console.
   */
  fun getRemoveAdvice(): String? {
    val unusedDependencies = consoleReport.removeAdvice
    if (unusedDependencies.isEmpty()) return null
    return unusedDependencies.mapToOrderedSet {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }.join("Unused dependencies which should be removed")
  }

  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  fun getAddAdvice(): String? {
    val undeclaredApiDeps = consoleReport.addToApiAdvice
    val undeclaredImplDeps = consoleReport.addToImplAdvice

    if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) return null

    val apiAdvice = undeclaredApiDeps.mapToOrderedSet {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)})"
    }.joinToString(prefix = "  ", separator = "\n  ")
    val implAdvice = undeclaredImplDeps.mapToOrderedSet {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)})"
    }.joinToString(prefix = "  ", separator = "\n  ")

    val header = "Transitively used dependencies that should be declared directly as indicated:\n"
    return if (undeclaredApiDeps.isNotEmpty() && undeclaredImplDeps.isNotEmpty()) {
      "$header$apiAdvice\n$implAdvice\n"
    } else if (undeclaredApiDeps.isNotEmpty()) {
      "$header$apiAdvice\n"
    } else if (undeclaredImplDeps.isNotEmpty()) {
      "$header$implAdvice\n"
    } else {
      // One or the other list must be non-empty
      throw GradleException("Impossible")
    }
  }

  /**
   * Returns "change-advice" (or null if none) for printing to console.
   */
  fun getChangeAdvice(): String? {
    val changeToApi = consoleReport.changeToApiAdvice
    val changeToImpl = consoleReport.changeToImplAdvice

    if (changeToApi.isEmpty() && changeToImpl.isEmpty()) return null

    val apiAdvice = changeToApi.mapToOrderedSet {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }.joinToString(prefix = "  ", separator = "\n  ")
    val implAdvice = changeToImpl.mapToOrderedSet {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }.joinToString(prefix = "  ", separator = "\n  ")
    val header = "Existing dependencies which should be modified to be as indicated:\n"
    return if (changeToApi.isNotEmpty() && changeToImpl.isNotEmpty()) {
      "$header$apiAdvice\n$implAdvice\n"
    } else if (changeToApi.isNotEmpty()) {
      "$header$apiAdvice\n"
    } else if (changeToImpl.isNotEmpty()) {
      "$header$implAdvice\n"
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
    if (compileOnlyDependencies.isEmpty()) return null
    return compileOnlyDependencies.mapToOrderedSet {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }.join("Dependencies which could be compile-only")
  }

  fun getRemoveProcAdvice(): String? {
    val unusedProcs = consoleReport.unusedProcsAdvice
    if (unusedProcs.isEmpty()) return null
    return unusedProcs.mapToOrderedSet {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }.join("Unused annotation processors that should be removed")
  }

  private fun getPluginAdvice(): String? {
    if (consoleReport.pluginAdvice.isEmpty()) return null
    return consoleReport.pluginAdvice.mapToOrderedSet {
      "${it.redundantPlugin}: ${it.reason}"
    }.join("Plugin advice")
  }

  private fun <T> Iterable<T>.join(header: CharSequence, transform: ((T) -> CharSequence)? = null): String {
    return joinToString(prefix = "$header:\n  ", postfix = "\n", separator = "\n  ", transform = transform)
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      val dependencyId = "${dependency.identifier}:${dependency.resolvedVersion}"
      dependencyRenamingMap?.getOrDefault(dependencyId, null) ?: "\"$dependencyId\""
    }

  fun consoleText(): String {
    var didAppend = false

    fun StringBuilder.appendAdvice(advice: String?): StringBuilder {
      if (advice != null) {
        if (didAppend) {
          appendReproducibleNewLine()
          didAppend = false
        }
        append(advice)
        didAppend = true
      }
      return this
    }

    return StringBuilder()
      .appendAdvice(getRemoveAdvice())
      .appendAdvice(getAddAdvice())
      .appendAdvice(getChangeAdvice())
      .appendAdvice(getCompileOnlyAdvice())
      .appendAdvice(getRemoveProcAdvice())
      .appendAdvice(getPluginAdvice())
      .toString()
  }
}
