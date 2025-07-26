// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.AndroidAssetSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

public abstract class AssetSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
) : DefaultTask() {

  init {
    description = "Produces a report of all assets in this project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidLocalAssets: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      projectDir.set(layout.projectDirectory)
      androidLocalAssets.setFrom(this@AssetSourceExploderTask.androidLocalAssets)
      output.set(this@AssetSourceExploderTask.output)
    }
  }

  public interface Parameters : WorkParameters {
    public val projectDir: DirectoryProperty
    public val androidLocalAssets: ConfigurableFileCollection
    public val output: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      val output = parameters.output.getAndDelete()
      val projectDir = parameters.projectDir.get().asFile

      val assets = parameters.androidLocalAssets.mapToOrderedSet { file ->
        AndroidAssetSource(relativePath = file.toRelativeString(projectDir))
      }

      output.bufferWriteJsonSet(assets)
    }
  }
}
