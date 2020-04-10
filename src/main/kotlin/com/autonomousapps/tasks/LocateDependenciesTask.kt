package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.DependencyConfiguration
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class LocateDependenciesTask @Inject constructor(objects: ObjectFactory): DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of all dependencies and the configurations on which they are declared"
  }

  @get:Input
  val dependencyConfigurations = objects.setProperty(DependencyConfiguration::class.java)

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.get().asFile
    outputFile.delete()

    val locations = dependencyConfigurations.get()
    outputFile.writeText(locations.toJson())
  }
}
