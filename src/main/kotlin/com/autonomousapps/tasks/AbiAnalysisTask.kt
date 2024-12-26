// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.kotlin.computeAbi
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterToClassFiles
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.jar.JarFile
import javax.inject.Inject

@CacheableTask
abstract class AbiAnalysisTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : AndroidClassesTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of the ABI of this project"
  }

  /** Class files generated by any JVM source (Java, Kotlin, Groovy, etc.). May be empty. */
  @get:Classpath
  @get:InputFiles
  abstract val classes: ConfigurableFileCollection

  @get:Optional
  @get:Input
  abstract val exclusions: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val abiDump: RegularFileProperty

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(AbiAnalysisWorkAction::class.java) {
      // JVM projects
      classFiles.setFrom(classes.asFileTree.filterToClassFiles().files)
      // Android projects
      classFiles.from(androidClassFiles())
      jarFiles.from(androidJarFiles())

      exclusions.set(this@AbiAnalysisTask.exclusions)
      output.set(this@AbiAnalysisTask.output)
      abiDump.set(this@AbiAnalysisTask.abiDump)
    }
  }

  interface AbiAnalysisParameters : WorkParameters {
    val classFiles: ConfigurableFileCollection
    val jarFiles: ConfigurableFileCollection
    val exclusions: Property<String>
    val output: RegularFileProperty
    val abiDump: RegularFileProperty
  }

  abstract class AbiAnalysisWorkAction : WorkAction<AbiAnalysisParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val outputAbiDump = parameters.abiDump.getAndDelete()

      val classFiles = parameters.classFiles.files
      val jarFiles = parameters.jarFiles.mapToSet { JarFile(it) }
      val exclusions = parameters.exclusions.orNull?.fromJson<AbiExclusions>() ?: AbiExclusions.NONE

      val explodingAbi = computeAbi(classFiles, jarFiles, exclusions, outputAbiDump)

      output.bufferWriteJsonSet(explodingAbi)
    }
  }
}
