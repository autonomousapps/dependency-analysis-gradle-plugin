package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Dependency
import org.gradle.api.GradleException

/**
 * Only concerned with human-readable advice meant to be printed to the console.
 */
internal class AdvicePrinter(private val computedAdvice: ComputedAdvice) {
  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  fun getAddAdvice(): String? {
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
  fun getRemoveAdvice(): String? {
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
  fun getChangeAdvice(): String? {
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
  fun getCompileOnlyAdvice(): String? {
    val compileOnlyDependencies = computedAdvice.compileOnlyDependencies

    if (compileOnlyDependencies.isEmpty()) {
      return null
    }

    return compileOnlyDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it)}) (was ${it.configurationName})"
    }
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      "\"${dependency.identifier}:${dependency.resolvedVersion}\""
    }
}