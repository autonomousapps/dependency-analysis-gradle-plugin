// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.Flags.shouldAnalyzeTests
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.utils.ModuleInfo
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.declaration.internal.Configurations.isForAnnotationProcessor
import com.autonomousapps.model.declaration.internal.Configurations.isForRegularDependency
import com.autonomousapps.model.declaration.internal.Declaration
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

@CacheableTask
abstract class FindDeclarationsTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies and the configurations on which they are declared"
  }

  @get:Input
  abstract val projectPath: Property<String>

  @get:Input
  abstract val shouldAnalyzeTest: Property<Boolean>

  @get:Nested
  abstract val declarationContainer: Property<DeclarationContainer>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()
    val declarations = Locator(declarationContainer.get()).declarations()
    output.bufferWriteJsonSet(declarations)
  }

  companion object {
    internal fun configureTask(
      task: FindDeclarationsTask,
      project: Project,
      outputPaths: NoVariantOutputPaths
    ) {
      val shouldAnalyzeTests = project.shouldAnalyzeTests()

      task.projectPath.set(project.path)
      task.shouldAnalyzeTest.set(shouldAnalyzeTests)
      task.declarationContainer.set(computeDeclarations(project, shouldAnalyzeTests))
      task.output.set(outputPaths.locationsPath)
    }

    private fun computeDeclarations(project: Project, shouldAnalyzeTests: Boolean): Provider<DeclarationContainer> {
      val configurations = project.configurations
      return project.provider {
        DeclarationContainer.of(
          mapping = getDependencyBuckets(configurations, shouldAnalyzeTests)
            .associateBy { it.name }
            .map { (name, conf) ->
              name to conf.dependencies.toIdentifiers()
            }
            .toMap()
        )
      }
    }

    private fun getDependencyBuckets(
      configurations: ConfigurationContainer,
      shouldAnalyzeTests: Boolean
    ): Sequence<Configuration> {
      val seq = configurations.asSequence()
        .filter { it.isForRegularDependency() || it.isForAnnotationProcessor() }

      return if (shouldAnalyzeTests) seq
      else seq.filterNot { it.name.startsWith("test") }
    }
  }

  class DeclarationContainer(
    @get:Input
    val mapping: Map<String, Set<Pair<ModuleInfo, GradleVariantIdentification>>>
  ) {

    companion object {
      internal fun of(
        mapping: Map<String, Set<Pair<ModuleInfo, GradleVariantIdentification>>>
      ): DeclarationContainer {
        // We sort the map so that the task input property is consistent in different environments
        return DeclarationContainer(mapping.toSortedMap())
      }
    }
  }

  private class Locator(private val declarationContainer: DeclarationContainer) {
    fun declarations(): Set<Declaration> {
      return declarationContainer.mapping.asSequence()
        .flatMap { (conf, identifiers) ->
          identifiers.map { id ->
            Declaration(
              identifier = id.first.identifier,
              version = id.first.version,
              configurationName = conf,
              gradleVariantIdentification = id.second
            )
          }
        }
        .sortedWith(compareBy<Declaration> { it.configurationName }
          .thenComparing { it -> it.identifier })
        .toSet()
    }
  }
}
