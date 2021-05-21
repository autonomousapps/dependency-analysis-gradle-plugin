package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.ConsoleReport
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
    return unusedDependencies.join("Unused dependencies which should be removed") {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }
  }

  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  fun getAddAdvice(): String? {
    val undeclaredApiDeps = consoleReport.addToApiAdvice
    val undeclaredImplDeps = consoleReport.addToImplAdvice

    if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) return null

    val apiAdvice = undeclaredApiDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)})"
    }
    val implAdvice = undeclaredImplDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)})"
    }

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

    val apiAdvice = changeToApi.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }
    val implAdvice = changeToImpl.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.toConfiguration}(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }
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
    return compileOnlyDependencies.join("Dependencies which could be compile-only") {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it.dependency)}) (was ${it.fromConfiguration})"
    }
  }

  fun getRemoveProcAdvice(): String? {
    val unusedProcs = consoleReport.unusedProcsAdvice
    if (unusedProcs.isEmpty()) return null
    return unusedProcs.join("Unused annotation processors that should be removed") {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }
  }

  private fun getPluginAdvice(): String? {
    if (consoleReport.pluginAdvice.isEmpty()) return null
    return consoleReport.pluginAdvice.join("Plugin advice") {
      "${it.redundantPlugin}: ${it.reason}"
    }
  }

  private fun <T> Iterable<T>.join(header: CharSequence, transform: ((T) -> CharSequence)? = null): String {
    return joinToString(prefix = "$header:\n- ", postfix = "\n", separator = "\n- ", transform = transform)
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      val dependencyId = "${dependency.identifier}:${dependency.resolvedVersion}"
      dependencyRenamingMap?.getOrDefault(dependencyId, null) ?: "\"$dependencyId\""
    }

  fun consoleText(): String {
    var didGiveAdvice = false
    var didAppend = false

    fun StringBuilder.appendAdvice(advice: String?): StringBuilder {
      if (advice != null) {
        if (didAppend) {
          appendReproducibleNewLine()
          didAppend = false
        }
        append(advice)
        didGiveAdvice = true
        didAppend = true
      }
      return this
    }

    val consoleReportText = StringBuilder()

    consoleReportText
      .appendAdvice(getRemoveAdvice())
      .appendAdvice(getAddAdvice())
      .appendAdvice(getChangeAdvice())
      .appendAdvice(getCompileOnlyAdvice())
      .appendAdvice(getRemoveProcAdvice())
      .appendAdvice(getPluginAdvice())

    return consoleReportText.toString()
  }
}
