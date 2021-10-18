package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ConfigurationsToDependenciesTransformer
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class LocateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all dependencies and the configurations on which they are declared"
  }

  @get:Optional
  @get:Input
  abstract val flavorName: Property<String>

  @get:Optional
  @get:Input
  abstract val buildType: Property<String>

  @get:Input
  abstract val variantName: Property<String>

  @get:Internal
  lateinit var configurations: ConfigurationContainer

  @Input
  fun getDeclaredDependencies(): Map<String, Set<String>> {
    return configurations.asMap.map { (name, conf) ->
      name to conf.dependencies.toIdentifiers()
    }.toMap()
  }

  /*
   * Outputs
   */

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val locations = ConfigurationsToDependenciesTransformer(
      flavorName = flavorName.orNull,
      buildType = buildType.orNull,
      variantName = variantName.get(),
      configurations = configurations
    ).locations()

    outputFile.writeText(locations.toJson())
  }
}
