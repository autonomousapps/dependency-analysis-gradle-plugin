package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
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
    group = TASK_GROUP_DEP_INTERNAL
    description =
      "Produces a report of all dependencies and the configurations on which they are declared"

    // This task can never be up to date because we do not yet know a way to model having the
    // configurations themselves (not the files they resolve to!) as an input
    // TODO May no longer be necessary now that an input is the resolved dependencies
    outputs.upToDateWhen { false }
  }

  @get:Optional
  @get:Input
  abstract val flavorName: Property<String>

  @get:Optional
  @get:Input
  abstract val buildType: Property<String>

  @get:Input
  abstract val variantName: Property<String>

  @get:Input
  abstract val includeTest: Property<Boolean>

  //  For up to date correctness
//  @get:Classpath
//  abstract val compileClasspathArtifacts: ConfigurableFileCollection

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
      configurations = project.configurations,
      includeTest = includeTest.get()
    ).locations()

    outputFile.writeText(locations.toJson())
  }
}
