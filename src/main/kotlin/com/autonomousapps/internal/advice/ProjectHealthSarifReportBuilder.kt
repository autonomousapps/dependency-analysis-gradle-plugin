// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.SourcedProjectAdvice
import io.github.detekt.sarif4k.*

internal class ProjectHealthSarifReportBuilder(
  projectAdvices: Collection<SourcedProjectAdvice>,
  dslKind: DslKind,
  /** Customize how dependencies are printed. */
  dependencyMap: ((String) -> String?)? = null,
  useTypesafeProjectAccessors: Boolean,
) {

  val sarif: SarifSchema210

  private val advicePrinter = AdvicePrinter(dslKind, dependencyMap, useTypesafeProjectAccessors)

  init {
    val pluginResults = projectAdvices.flatMap { projectAdvice ->
      projectAdvice.pluginAdvice.map { advice ->
        val location = projectAdvice.projectBuildFile?.let { buildFile ->
          Location(
            physicalLocation = PhysicalLocation(
              artifactLocation = ArtifactLocation(uri = buildFile),
            ),
          )
        }

        Result(
          locations = listOfNotNull(location),
          message = Message(text = "Pluigin ${advice.redundantPlugin} should be removed: ${advice.reason}"),
          ruleID = "dependencyAnalysis.Plugin"
        )
      }

    }
    val dependencyResults = projectAdvices.flatMap { projectAdvice ->
      projectAdvice.dependencyAdvice.map { sourcedAdvice ->
        val message: String
        val ruleId: String

        val advice = sourcedAdvice.advice
        when {
          advice.isAdd() -> {
            message = "Transitive dependency ${advicePrinter.toDeclaration(advice).trim()} should be declared directly"
            ruleId = "dependencyanalysis.Add"
          }

          advice.isRemove() -> {
            message = "Unused dependency ${advicePrinter.fromDeclaration(advice).trim()} should be removed"
            ruleId = "dependencyanalysis.Remove"
          }

          advice.isChange() -> {
            message =
              "Dependency ${
                advicePrinter.fromDeclaration(advice).trim()
              } should be modified to ${advice.toConfiguration} from ${advice.fromConfiguration}"
            ruleId = "dependencyanalysis.Change"
          }

          advice.isChangeToRuntimeOnly() -> {
            message =
              "Dependency ${advicePrinter.fromDeclaration(advice).trim()} should be removed or changed to runtime-only"
            ruleId = "dependencyanalysis.ChangeRuntimeOnly"
          }

          advice.isCompileOnly() -> {
            message = "Dependency ${advicePrinter.fromDeclaration(advice).trim()} should be changed to compile-only"
            ruleId = "dependencyanalysis.ChangeCompileOnly"
          }

          advice.isProcessor() -> {
            message = "Unused annotation processor ${advicePrinter.fromDeclaration(advice).trim()} should be removed"
            ruleId = "dependencyanalysis.Processpr"
          }

          else -> {
            error("Unknown advice type: $advice")
          }
        }

        val location = projectAdvice.projectBuildFile?.let { buildFile ->
          Location(
            physicalLocation = PhysicalLocation(
              artifactLocation = ArtifactLocation(uri = buildFile),
              region =
                Region(
                  startLine = sourcedAdvice.buildFileDeclarationLineNumber?.toLong(),
                  endLine = sourcedAdvice.buildFileDeclarationLineNumber?.toLong(),
                )
            ),
          )
        }

        Result(
          locations = listOfNotNull(location),
          message = Message(text = message),
          ruleID = ruleId
        )
      }
    }

    sarif = SarifSchema210(
      schema = "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json",
      version = Version.The210,
      runs = listOf(
        Run(
          results = dependencyResults + pluginResults,
          tool = SARIF_TOOL,
        )
      )
    )
  }
}

private val SARIF_TOOL = Tool(
  driver = ToolComponent(
    guid = "f9137358-f4fb-44f2-8300-39ca0b85fb77",
    informationURI = "https://github.com/autonomousapps/dependency-analysis-gradle-plugin",
    language = "en",
    name = "dependency-analysis-gradle-plugin",
    rules = listOf(
      ReportingDescriptor(
        id = "dependencyanalysis.Add",
        name = "Add",
        shortDescription = MultiformatMessageString(
          text = "These transitive dependencies should be declared directly"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.Remove",
        name = "Remove",
        shortDescription = MultiformatMessageString(
          text = "Unused dependencies which should be removed"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.Change",
        name = "Change",
        shortDescription = MultiformatMessageString(
          text = "Existing dependencies which should be modified to be as indicated"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.ChangeRuntimeOnly",
        name = "ChangeRuntimeOnly",
        shortDescription = MultiformatMessageString(
          text = "Dependencies which should be removed or changed to runtime-only"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.ChangeCompileOnly",
        name = "ChangeCompileOnly",
        shortDescription = MultiformatMessageString(
          text = "Dependencies which could be compile-only"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.Processor",
        name = "Processor",
        shortDescription = MultiformatMessageString(
          text = "Unused annotation processors that should be removed"
        )
      ),
      ReportingDescriptor(
        id = "dependencyanalysis.Plugin",
        name = "Plugin",
        shortDescription = MultiformatMessageString(
          text = "Unused plugins that can be removed"
        )
      ),
    ),
  )
)
