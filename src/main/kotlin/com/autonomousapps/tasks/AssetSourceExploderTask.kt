package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.AndroidAssetSource
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

abstract class AssetSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all assets in this project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidLocalAssets: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      projectDir.set(layout.projectDirectory)
      androidLocalAssets.setFrom(this@AssetSourceExploderTask.androidLocalAssets)
      output.set(this@AssetSourceExploderTask.output)
    }
  }

  interface Parameters : WorkParameters {
    val projectDir: DirectoryProperty
    val androidLocalAssets: ConfigurableFileCollection
    val output: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      val output = parameters.output.getAndDelete()
      val projectDir = parameters.projectDir.get().asFile

      val assets = parameters.androidLocalAssets.mapToSet { file ->
        AndroidAssetSource(relativePath = file.toRelativeString(projectDir))
      }

      output.writeText(assets.toJson())
    }
  }
}
