// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.ProjectType
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.Colors
import com.autonomousapps.internal.utils.Colors.colorize
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.*
import java.util.*

internal class ProjectHealthConsoleReportBuilder(
  private val projectAdvice: ProjectAdvice,
  private val postscript: String,
  projectMetadata: ProjectMetadata,
  dslKind: DslKind,
  /** Customize how dependencies are printed. */
  dependencyMap: ((String) -> String?)? = null,
  useTypesafeProjectAccessors: Boolean,
) {

  val text: String

  private val projectType = projectMetadata.projectType

  private val advicePrinter = AdvicePrinter(
    dslKind = dslKind,
    projectType = projectMetadata.projectType,
    dependencyMap = dependencyMap,
    useTypesafeProjectAccessors = useTypesafeProjectAccessors,
  )
  private var shouldPrintNewLine = false

  init {
    val dependencyAdvice = projectAdvice.dependencyAdvice
    val removeAdvice = mutableSetOf<Advice>()
    val addAdvice = mutableSetOf<Advice>()
    val changeAdvice = mutableSetOf<Advice>()
    val runtimeOnlyAdvice = mutableSetOf<Advice>()
    val compileOnlyAdvice = mutableSetOf<Advice>()
    val processorAdvice = mutableSetOf<Advice>()

    dependencyAdvice.forEach { advice ->
      if (advice.isRemove()) removeAdvice += advice
      if (advice.isAdd()) addAdvice += advice
      if (advice.isChange()) changeAdvice += advice
      if (advice.isChangeToRuntimeOnly()) runtimeOnlyAdvice += advice
      if (advice.isCompileOnly()) compileOnlyAdvice += advice
      if (advice.isProcessor()) processorAdvice += advice
    }

    text = buildString {
      if (removeAdvice.isNotEmpty()) {
        // This is the first printed advice, so we don't print new lines here.
        shouldPrintNewLine = true
        appendReproducibleNewLine("Unused dependencies which should be removed:")

        val toPrint = printAdvice(removeAdvice) { a ->
          Triple(a.fromConfiguration!!, printableIdentifier(a.coordinates), "")
        }
        append(toPrint)
      }

      if (addAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("These transitive dependencies should be declared directly:")

        val toPrint = printAdvice(addAdvice) { a ->
          Triple(a.toConfiguration!!, printableIdentifier(a.coordinates), "")
        }
        append(toPrint)
      }

      if (changeAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("Existing dependencies which should be modified to be as indicated:")

        val toPrint = printAdvice(changeAdvice) { a ->
          Triple(a.toConfiguration!!, printableIdentifier(a.coordinates), " (was ${a.fromConfiguration})")
        }
        append(toPrint)
      }

      if (runtimeOnlyAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("Dependencies which should be removed or changed to runtime-only:")

        val toPrint = printAdvice(runtimeOnlyAdvice) { a ->
          Triple(a.toConfiguration!!, printableIdentifier(a.coordinates), " (was ${a.fromConfiguration})")
        }
        append(toPrint)
      }

      if (compileOnlyAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("Dependencies which could be compile-only:")

        val toPrint = printAdvice(compileOnlyAdvice) { a ->
          Triple(a.toConfiguration!!, printableIdentifier(a.coordinates), " (was ${a.fromConfiguration})")
        }
        append(toPrint)
      }

      if (processorAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("Unused annotation processors that should be removed:")

        val toPrint = printAdvice(processorAdvice) { a ->
          Triple(a.fromConfiguration!!, printableIdentifier(a.coordinates), "")
        }
        append(toPrint)
      }

      val pluginAdvice = projectAdvice.pluginAdvice
      if (pluginAdvice.isNotEmpty()) {
        maybeAppendTwoLines()
        appendReproducibleNewLine("Unused plugins that can be removed:")

        val toPrint = pluginAdvice.mapToOrderedSet {
          "  ${it.redundantPlugin}: ${it.reason}"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      appendModuleAdvice()
      appendWarnings()
      appendPostscript()
    }.trimEnd()
  }

  private fun StringBuilder.maybeAppendTwoLines() {
    if (shouldPrintNewLine) {
      appendReproducibleNewLine()
      appendReproducibleNewLine()
    }
    shouldPrintNewLine = true
  }

  private fun StringBuilder.appendModuleAdvice() {
    val moduleAdvice = projectAdvice.moduleAdvice
    if (!moduleAdvice.hasPrintableAdvice()) return

    if (moduleAdvice.isNotEmpty()) {
      maybeAppendTwoLines()
      appendReproducibleNewLine("Module structure advice")

      moduleAdvice.forEach { m ->
        when (m) {
          is AndroidScore -> if (m.couldBeJvm()) append(m.text())
        }
      }
    }
  }

  private fun StringBuilder.appendWarnings() {
    val duplicateClasses = projectAdvice.warning.duplicateClasses
    if (duplicateClasses.isEmpty()) return

    maybeAppendTwoLines()
    appendReproducibleNewLine("Warnings")

    appendReproducibleNewLine("Some of your classpaths have duplicate classes, which means the compile and runtime behavior can be sensitive to the classpath order.")
    appendReproducibleNewLine()

    duplicateClasses
      .mapToOrderedSet { it.sourceKind.name }
      .forEachIndexed { i, v ->
        if (i > 0) appendReproducibleNewLine()

        appendReproducibleNewLine("Source set: $v")

        val duplicatesByVariant = duplicateClasses.filter { it.sourceKind.name == v }

        duplicatesByVariant
          .mapToOrderedSet { it.classpathName }
          .forEach { c ->
            "$c classpath".let { txt ->
              append("\\--- ")
              appendReproducibleNewLine(txt)
            }

            val duplicatesByClasspath = duplicatesByVariant.filter { it.classpathName == c }
            duplicatesByClasspath
              .filter { it.classpathName == c }
              .forEachIndexed { i, d ->
                // TODO(tsr): print capabilities too
                val deps = d.dependencies
                  .map { if (it is IncludedBuildCoordinates) it.resolvedProject else it }
                  .map { it.gav() }

                if (duplicatesByClasspath.size > 1 && i < duplicatesByClasspath.size - 1) {
                  append("     +--- ")
                } else {
                  append("     \\--- ")
                }

                appendReproducibleNewLine("${d.className} is provided by multiple dependencies: $deps")
              }
          }
      }
  }

  private fun StringBuilder.appendPostscript() {
    // Only print the postscript if there is anything at all to report.
    if (isEmpty() || postscript.isEmpty()) return

    maybeAppendTwoLines()
    appendReproducibleNewLine(postscript.colorize(Colors.BOLD))
  }

  private fun Set<ModuleAdvice>.hasPrintableAdvice(): Boolean {
    return ModuleAdvice.isNotEmpty(this)
  }

  private fun AndroidScore.text() = buildString {
    if (shouldBeJvm()) {
      appendReproducibleNewLine("This project doesn't use any Android features and should be a JVM project.")
    } else {
      appendReproducibleNewLine("This project uses limited Android features and could be a JVM project.")
      if (usesAndroidClasses) appendReproducibleNewLine("* Uses Android classes.")
      if (hasAndroidRes) appendReproducibleNewLine("* Uses Android resources.")
      if (hasAndroidAssets) appendReproducibleNewLine("* Contains Android assets.")
      if (hasBuildConfig) appendReproducibleNewLine("* Includes BuildConfig.")
      if (hasAndroidDependencies) appendReproducibleNewLine("* Has Android library dependencies.")
    }
  }

  private fun printAdvice(advice: Set<Advice>, transform: (Advice) -> Triple<String, String, String>): String {
    // non-KMP is much simpler. Every piece of advice gets a single line.
    if (projectType != ProjectType.KMP) {
      return advice.mapToOrderedSet { a ->
        val data = transform(a)
        line(data.first, data.second, data.third)
      }.joinToString(separator = "\n")
    }

    /*
     * KMP case. Each piece of advice is aggregated into source sets to make it a bit more compact.
     */

    val unknownKey = "__UNKNOWN"

    // Put the advice into a map with the sourceSetName as the key
    val adviceBySourceSetName = sortedMapOf<String, SortedSet<Advice>>()
    advice
      .forEach { a ->
        val data = transform(a)
        val key = DependencyScope.sourceSetName(data.first) ?: unknownKey

        adviceBySourceSetName.merge(key, sortedSetOf(a)) { acc, inc ->
          acc.apply { addAll(inc) }
        }
      }

    val builder = StringBuilder()
    var shouldPrintNewLine = false

    // For well-known scopes
    adviceBySourceSetName
      .filterKeys { it != unknownKey }
      .forEach { (sourceSetName, advice) ->
        if (shouldPrintNewLine) {
          builder.appendLine()
        }

        shouldPrintNewLine = true

        builder
          .append("  ")
          .append(sourceSetName)
          .appendLine(".dependencies {")

        advice.forEach { a ->
          val data = transform(a)
          // "jvmMainImplementation"
          val kmpConfigurationName = data.first
            // => "Implementation"
            .substringAfter(sourceSetName)
            // => "implementation"
            .replaceFirstChar(Char::lowercase)

          val line = line(kmpConfigurationName, data.second, data.third)
          builder
            .append("  ")
            .appendLine(line)
        }

        builder.append("  }")
      }

    val unknownScopes = adviceBySourceSetName.filterKeys { it == unknownKey }
    if (unknownScopes.isNotEmpty()) {
      builder.appendLine()
    }

    shouldPrintNewLine = false

    // For unknown scopes
    unknownScopes
      .forEach { (_, advice) ->
        advice.forEach { a ->
          if (shouldPrintNewLine) {
            builder.appendLine()
          }
          shouldPrintNewLine = true

          val data = transform(a)
          val line = line(data.first, data.second, data.third)
          builder.append(line)
        }
      }

    return builder.toString()
  }

  private fun line(configuration: String, printableIdentifier: String, was: String = ""): String {
    return advicePrinter.line(configuration, printableIdentifier, was)
  }

  private fun printableIdentifier(coordinates: Coordinates) = advicePrinter.gav(coordinates)
}
