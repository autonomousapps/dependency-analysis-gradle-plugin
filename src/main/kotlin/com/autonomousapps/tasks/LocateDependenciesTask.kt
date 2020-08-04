package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.ConfigurationsToDependenciesTransformer
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class LocateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of all dependencies and the configurations on which they are declared"
  }

  @get:Optional
  @get:Input
  abstract val flavorName: Property<String>

  @get:Input
  abstract val variantName: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val locations = ConfigurationsToDependenciesTransformer(
      flavorName = flavorName.orNull,
      variantName = variantName.get(),
      project = project
    ).dependencyConfigurations()

    outputFile.writeText(locations.toJson())
  }
}
