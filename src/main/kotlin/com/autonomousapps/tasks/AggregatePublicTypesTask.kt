// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.PublicTypes
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingAbi
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
public abstract class AggregatePublicTypesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = "Writes report of all public classes across all variants or source sets in this project"
    description = "Writes report of all public classes across all variants or source sets in this project"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val abiReports: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.projectPath.set(projectPath)
      it.abiReports.setFrom(abiReports)
      it.output.set(output)
    }
  }

  public interface Parameters : WorkParameters {
    public val projectPath: Property<String>
    public val abiReports: ConfigurableFileCollection
    public val output: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reports = parameters.abiReports.flatMap { it.fromJsonSet<ExplodingAbi>() }
      val classNames = reports.mapToOrderedSet { it.className }
      val publicTypes = PublicTypes(parameters.projectPath.get(), classNames)

      output.bufferWriteJson(publicTypes)
    }
  }
}
