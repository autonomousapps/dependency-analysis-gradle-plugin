package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.configuration.Configurations.isAnnotationProcessor
import com.autonomousapps.internal.configuration.Configurations.isMain
import com.autonomousapps.internal.configuration.Configurations.isMainStrict
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.intermediates.Attribute
import com.autonomousapps.model.intermediates.Declaration
import com.autonomousapps.shouldAnalyzeTests
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
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
  abstract val locationContainer: Property<LocationContainer>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()
    val locations = Locator(locationContainer.get()).locations()
    output.writeText(locations.toJson())
  }

  companion object {
    internal fun configureTask(
      task: FindDeclarationsTask,
      project: Project,
      outputPaths: NoVariantOutputPaths
    ) {
      task.projectPath.set(project.path)
      task.shouldAnalyzeTest.set(project.shouldAnalyzeTests())
      task.locationContainer.set(computeLocations(project))
      task.output.set(outputPaths.locationsPath)
    }

    private fun computeLocations(project: Project): Provider<LocationContainer> {
      val configurations = if (project.shouldAnalyzeTests()) project.configurations.filter { true } else project.configurations.filter { configuration -> isMainStrict(configuration.name) }
      return project.provider {
        val metadata = mutableMapOf<String, Boolean>()
        LocationContainer.of(
          mapping = getDependencyBuckets(configurations)
            .associateBy { it.name }
            .map { (name, conf) ->
              name to conf.dependencies.toIdentifiers(metadata)
            }
            .toMap(),
          metadata = LocationMetadata.of(metadata)
        )
      }
    }

    private fun getDependencyBuckets(configurations: List<Configuration>): Sequence<Configuration> {
      return configurations.asSequence().filter { it.isMain() || it.isAnnotationProcessor() }
    }

    // we want dependency buckets only
    private fun Configuration.isMain() = !isCanBeConsumed && !isCanBeResolved && isMain(name)

    // as in so many things, "kapt" is special: it is a resolvable configuration
    private fun Configuration.isAnnotationProcessor() = isAnnotationProcessor(name)
  }
}

class LocationContainer(
  @get:Input
  val mapping: Map<String, Set<String>>,
  @get:Nested
  val metadata: LocationMetadata
) {

  companion object {
    internal fun of(
      mapping: Map<String, Set<String>>,
      metadata: LocationMetadata
    ): LocationContainer = LocationContainer(mapping, metadata)
  }
}

class LocationMetadata(
  @get:Input
  val metadata: Map<String, Boolean>
) {

  internal fun attributes(id: String): Set<Attribute> {
    return if (isJavaPlatform(id)) setOf(Attribute.JAVA_PLATFORM) else emptySet()
  }

  private fun isJavaPlatform(id: String): Boolean = metadata.containsKey(id)

  companion object {
    internal fun of(metadata: Map<String, Boolean>): LocationMetadata = LocationMetadata(metadata)
  }
}

internal class Locator(private val locationContainer: LocationContainer) {
  fun locations(): Set<Declaration> {
    return locationContainer.mapping.asSequence()
      .filter { (name, _) -> isMain(name) || isAnnotationProcessor(name) }
      .flatMap { (conf, identifiers) ->
        identifiers.map { id ->
          Declaration(
            identifier = id,
            configurationName = conf,
            attributes = locationContainer.metadata.attributes(id)
          )
        }
      }
      .toSet()
  }
}
