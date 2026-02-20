// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.Flags.shouldAnalyzeTests
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.DeclarationContainer
import com.autonomousapps.model.internal.declaration.Locator
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

@CacheableTask
public abstract class FindDeclarationsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies and the configurations on which they are declared"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Input
  public abstract val projectType: Property<ProjectType>

  @get:Input
  public abstract val shouldAnalyzeTest: Property<Boolean>

  @get:Nested
  public abstract val declarationContainer: Property<DeclarationContainer>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()
    val declarations = Locator(declarationContainer.get()).declarations()
    output.bufferWriteJsonSet(declarations)
  }

  internal companion object {
    fun configureTask(
      task: FindDeclarationsTask,
      project: Project,
      projectType: ProjectType,
      supportedSourceSetNames: Provider<Set<String>>,
      outputPaths: NoVariantOutputPaths
    ) {
      val shouldAnalyzeTests = project.shouldAnalyzeTests()

      task.projectPath.set(project.path)
      task.projectType.set(projectType)
      task.shouldAnalyzeTest.set(shouldAnalyzeTests)
      task.declarationContainer.set(
        computeDeclarations(
          project = project,
          projectType = projectType,
          supportedSourceSetNames = supportedSourceSetNames,
          shouldAnalyzeTests = shouldAnalyzeTests,
        )
      )
      task.output.set(outputPaths.locationsPath)
    }

    private fun computeDeclarations(
      project: Project,
      projectType: ProjectType,
      supportedSourceSetNames: Provider<Set<String>>,
      shouldAnalyzeTests: Boolean,
    ): Provider<DeclarationContainer> {
      val configurations = project.configurations
      return project.provider {
        DeclarationContainer.of(
          configurations = configurations,
          configurationNames = ConfigurationNames(projectType, supportedSourceSetNames.get()),
          shouldAnalyzeTests = shouldAnalyzeTests,
        )
      }
    }
  }
}
