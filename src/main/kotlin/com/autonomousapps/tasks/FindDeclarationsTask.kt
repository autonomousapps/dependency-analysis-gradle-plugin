package com.autonomousapps.tasks

import com.autonomousapps.Flags.shouldAnalyzeTests
import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.declaration.Configurations.isForAnnotationProcessor
import com.autonomousapps.model.declaration.Configurations.isForRegularDependency
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.declaration.Declaration.Attribute
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
    group = TASK_GROUP_DEP_INTERNAL
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
    output.writeText(declarations.toJson())
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
      task.declarationContainer.set(computeLocations(project, shouldAnalyzeTests))
      task.output.set(outputPaths.locationsPath)
    }

    private fun computeLocations(project: Project, shouldAnalyzeTests: Boolean): Provider<DeclarationContainer> {
      val configurations = project.configurations
      return project.provider {
        val metadata = mutableMapOf<String, Boolean>()
        DeclarationContainer.of(
          mapping = getDependencyBuckets(configurations, shouldAnalyzeTests)
            .associateBy { it.name }
            .map { (name, conf) ->
              name to conf.dependencies.toIdentifiers(metadata)
            }
            .toMap(),
          metadata = DeclarationMetadata.of(metadata)
        )
      }
    }

    private fun getDependencyBuckets(
      configurations: ConfigurationContainer,
      shouldAnalyzeTests: Boolean
    ): Sequence<Configuration> {
      val seq = configurations.asSequence()
        .filter { it.isForRegularDependency() || it.isForAnnotationProcessor() }
        .filterNot {
          // this is not ideal, but it will solve some problems till we can support androidTest analysis.
          // will match, e.g., "androidTestImplementation", "debugAndroidTestImplementation", and "kaptAndroidTest".
          it.name.contains("androidTest", true)
        }

      return if (shouldAnalyzeTests) seq
      else seq.filterNot { it.name.startsWith("test") }
    }

    // we want dependency buckets only
    private fun Configuration.isForRegularDependency() =
      !isCanBeConsumed && !isCanBeResolved && isForRegularDependency(name)

    // as in so many things, "kapt" is special: it is a resolvable configuration
    private fun Configuration.isForAnnotationProcessor() = isForAnnotationProcessor(name)
  }
}

class DeclarationContainer(
  @get:Input val mapping: Map<String, Set<String>>,
  @get:Nested val metadata: DeclarationMetadata
) {

  companion object {
    internal fun of(
      mapping: Map<String, Set<String>>,
      metadata: DeclarationMetadata
    ): DeclarationContainer = DeclarationContainer(mapping, metadata)
  }
}

class DeclarationMetadata(
  @get:Input
  val metadata: Map<String, Boolean>
) {

  internal fun attributes(id: String): Set<Attribute> {
    return if (isJavaPlatform(id)) setOf(Attribute.JAVA_PLATFORM) else emptySet()
  }

  private fun isJavaPlatform(id: String): Boolean = metadata.containsKey(id)

  companion object {
    internal fun of(metadata: Map<String, Boolean>): DeclarationMetadata = DeclarationMetadata(metadata)
  }
}

internal class Locator(private val declarationContainer: DeclarationContainer) {
  fun declarations(): Set<Declaration> {
    return declarationContainer.mapping.asSequence()
      // .filter { (name, _) -> isForRegularDependency(name) || isForAnnotationProcessor(name) }
      .flatMap { (conf, identifiers) ->
        identifiers.map { id ->
          Declaration(
            identifier = id,
            configurationName = conf,
            attributes = declarationContainer.metadata.attributes(id)
          )
        }
      }
      .toSet()
  }
}
