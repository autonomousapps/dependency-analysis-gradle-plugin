// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ANNOTATION_PROCESSOR_PATH
import com.autonomousapps.internal.SERVICE_LOADER_PATH
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.filterNonGradle
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.intermediates.producer.ServiceLoaderDependency
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.util.zip.ZipFile

/**
 * See also [ServiceLoader][https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html].
 *
 * Some dependencies are loaded at runtime, and cannot reasonably be detected by looking at compiled
 * bytecode. We will special-case all such dependencies discovered on the classpath.
 */
@CacheableTask
public abstract class FindServiceLoadersTask : DefaultTask() {

  init {
    description = "Produces a report of all dependencies that include Java ServiceLoaders"
  }

  private lateinit var compileClasspath: ArtifactCollection

  public fun setCompileClasspath(artifacts: ArtifactCollection) {
    this.compileClasspath = artifacts
  }

  @Classpath
  public fun getCompileClasspath(): FileCollection = compileClasspath.artifactFiles

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    // TODO(tsr): there's a bug here. If a service loader is coming from another subproject in the same build, then
    //  compileClasspath contains a directory that includes only class files. It doesn't not contain any resources
    //  files, which is where the service loader definition would be.
    val serviceLoaders = compileClasspath
      .filterNonGradle()
      .filter { it.file.name.endsWith(".jar") }
      .flatMapToSet { findServiceLoaders(it) }

    outputFile.bufferWriteJsonSet(serviceLoaders)
  }

  // E.g. org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5 -->
  // 1. META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory
  // 2. META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler
  private fun findServiceLoaders(artifact: ResolvedArtifactResult): Set<ServiceLoaderDependency> {
    val zip = ZipFile(artifact.file)

    return zip.entries().asSequence()
      .filter { it.name.startsWith(SERVICE_LOADER_PATH) }
      .filterNot { it.name.startsWith(ANNOTATION_PROCESSOR_PATH) }
      .filterNot { it.isDirectory }
      .mapNotNull { serviceFile ->
        val providerClasses = zip.getInputStream(serviceFile)
          .bufferedReader().use(BufferedReader::readLines)
          // remove whitespace
          .map { it.trim() }
          // Ignore comments
          .filterToSet { !it.startsWith("#") }

        // Unclear why this would ever be empty.
        // See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/780
        if (providerClasses.isNotEmpty()) {
          ServiceLoaderDependency.newInstance(
            providerFile = serviceFile.name.removePrefix(SERVICE_LOADER_PATH),
            providerClasses = providerClasses,
            artifact = artifact
          )
        } else {
          val contents = zip.getInputStream(serviceFile).bufferedReader().use(BufferedReader::readText)
          logger.debug(
            "${artifact.file.name} has a services file at path ${serviceFile.name}, but there are no services! " +
              "File contents:\n<<$contents>>"
          )
          null
        }
      }.toSortedSet()
  }
}
