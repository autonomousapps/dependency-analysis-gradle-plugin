// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.parse.AndroidLayoutParser
import com.autonomousapps.internal.parse.AndroidManifestParser
import com.autonomousapps.internal.parse.AndroidResBuilder
import com.autonomousapps.internal.parse.AndroidResParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * TODO this kdoc is out of date.
 *
 * This task takes two inputs:
 * 1. Android res files declared by this project (xml)
 * 2. artifacts of type "android-public-res" (public.txt)
 *
 * We can parse the first for elements that might be present in the second. For example, if we have
 * ```
 * <resources>
 *   <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
 * </resources>
 * ```
 * we can expect to find, in public.txt, this line, associated with the dependency that supplies it (in this case
 * `'androidx.appcompat:appcompat'`):
 * ```
 * style Theme_AppCompat_Light_DarkActionBar
 * ```
 */
@CacheableTask
abstract class XmlSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
  private val objects: ObjectFactory
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all resources references in this project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidLocalRes: ConfigurableFileCollection

  /** Android layout XML files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val layoutFiles: ConfigurableFileCollection

  /** AndroidManifest.xml files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val manifestFiles: ConfigurableFileCollection

  @get:Input
  abstract val namespace: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  internal fun layouts(files: List<File>) {
    for (file in files) {
      layoutFiles.from(
        objects.fileTree().from(file)
          .matching {
            // At this point in the filtering, there's a mix of directories and files
            // Can't filter on file extension
            include { it.path.contains("layout") }
          }.files
          // At this point, we have only files. It is safe to filter on extension. We
          // only want XML files.
          .filter { it.extension == "xml" }
      )
    }
  }

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(XmlSourceExploderWorkAction::class.java) {
      projectDir.set(layout.projectDirectory)
      androidRes.setFrom(androidLocalRes)
      layouts.setFrom(layoutFiles)
      manifests.setFrom(manifestFiles)
      namespace.set(this@XmlSourceExploderTask.namespace)
      output.set(this@XmlSourceExploderTask.output)
    }
  }

  interface XmlSourceExploderParameters : WorkParameters {
    val projectDir: DirectoryProperty
    val androidRes: ConfigurableFileCollection
    val layouts: ConfigurableFileCollection
    val manifests: ConfigurableFileCollection
    val namespace: Property<String>
    val output: RegularFileProperty
  }

  abstract class XmlSourceExploderWorkAction : WorkAction<XmlSourceExploderParameters> {

    private val builders = mutableMapOf<String, AndroidResBuilder>()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val projectDir = parameters.projectDir.get().asFile
      val explodedLayouts = AndroidLayoutParser(
        layouts = parameters.layouts.files,
        projectDir = projectDir
      ).explodedLayouts
      val explodedResources = AndroidResParser(
        resources = parameters.androidRes,
        projectDir = projectDir
      ).androidResSource

      val explodedManifests = AndroidManifestParser(
        manifests = parameters.manifests,
        projectDir = projectDir,
        namespace = parameters.namespace.get(),
      ).explodedManifests

      explodedLayouts.forEach { explodedLayout ->
        builders.merge(
          explodedLayout.relativePath,
          AndroidResBuilder(explodedLayout.relativePath).apply {
            usedClasses.addAll(explodedLayout.usedClasses)
          },
          AndroidResBuilder::concat
        )
      }
      explodedResources.forEach { explodedRes ->
        builders.merge(
          explodedRes.relativePath,
          AndroidResBuilder(explodedRes.relativePath).apply {
            styleParentRefs.addAll(explodedRes.styleParentRefs)
            attrRefs.addAll(explodedRes.attrRefs)
          },
          AndroidResBuilder::concat
        )
      }
      explodedManifests.forEach { explodedManifest ->
        builders.merge(
          explodedManifest.relativePath,
          AndroidResBuilder(explodedManifest.relativePath).apply {
            if (explodedManifest.applicationName.isNotBlank()) {
              usedClasses.add(explodedManifest.applicationName)
            }
            explodedManifest.theme?.let { attrRefs.add(it) }
          },
          AndroidResBuilder::concat
        )
      }

      val androidResSource = builders.values.asSequence()
        .map { it.build() }
        .toSet()

      output.bufferWriteJsonSet(androidResSource)
    }
  }
}
