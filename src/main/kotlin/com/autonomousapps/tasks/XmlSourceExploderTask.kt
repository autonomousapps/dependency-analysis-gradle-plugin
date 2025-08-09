// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.parse.AndroidLayoutParser
import com.autonomousapps.internal.parse.AndroidManifestParser
import com.autonomousapps.internal.parse.AndroidResBuilder
import com.autonomousapps.internal.parse.AndroidResParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.AndroidResSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
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
public abstract class XmlSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
) : DefaultTask() {

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidLocalRes: ConfigurableFileCollection

  /** Android layout XML files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val layoutFiles: ConfigurableFileCollection

  /** AndroidManifest.xml files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val manifestFiles: ConfigurableFileCollection

  /** Merged AndroidManifest.xml files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val mergedManifestFiles: ConfigurableFileCollection

  @get:Input
  public abstract val namespace: Property<String>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  /** Elements not necessary at compile-time. */
  @get:OutputFile
  public abstract val outputRuntime: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(XmlSourceExploderWorkAction::class.java) {
      projectDir.set(layout.projectDirectory)
      androidRes.setFrom(androidLocalRes)
      layouts.setFrom(layoutFiles)
      manifests.setFrom(manifestFiles)
      mergedManifests.setFrom(mergedManifestFiles)
      namespace.set(this@XmlSourceExploderTask.namespace)
      output.set(this@XmlSourceExploderTask.output)
      outputRuntime.set(this@XmlSourceExploderTask.outputRuntime)
    }
  }

  public interface XmlSourceExploderParameters : WorkParameters {
    public val projectDir: DirectoryProperty
    public val androidRes: ConfigurableFileCollection
    public val layouts: ConfigurableFileCollection
    public val manifests: ConfigurableFileCollection
    public val mergedManifests: ConfigurableFileCollection
    public val namespace: Property<String>
    public val output: RegularFileProperty
    public val outputRuntime: RegularFileProperty
  }

  public abstract class XmlSourceExploderWorkAction : WorkAction<XmlSourceExploderParameters> {

    private val builders = mutableMapOf<String, AndroidResBuilder>()
    private val runtimeBuilders = mutableMapOf<String, AndroidResBuilder>()

    override fun execute() {
      val output = parameters.output.getAndDelete()
      val outputRuntime = parameters.outputRuntime.getAndDelete()

      val projectDir = parameters.projectDir.get().asFile

      val explodedResources = AndroidResParser(
        projectDir = projectDir,
        resources = parameters.androidRes,
      ).androidResSource
      val explodedLayouts = AndroidLayoutParser(
        projectDir = projectDir,
        layouts = parameters.layouts,
      ).explodedLayouts
      val explodedManifests = AndroidManifestParser(
        projectDir = projectDir,
        manifests = parameters.manifests,
        namespace = parameters.namespace.get(),
      ).explodedManifests
      val explodedMergeManifests = AndroidManifestParser(
        projectDir = projectDir,
        manifests = parameters.mergedManifests,
        namespace = parameters.namespace.get(),
      ).explodedManifests

      /*
       * Compile-time builders.
       */

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
            explodedManifest.attrRefs.forEach(attrRefs::add)
          },
          AndroidResBuilder::concat
        )
      }

      /*
       * Runtime builders.
       */

      explodedMergeManifests.forEach { explodedManifest ->
        runtimeBuilders.merge(
          explodedManifest.relativePath,
          AndroidResBuilder(explodedManifest.relativePath).apply {
            if (explodedManifest.applicationName.isNotBlank()) {
              usedClasses.add(explodedManifest.applicationName)
            }
            explodedManifest.attrRefs.forEach(attrRefs::add)
          },
          AndroidResBuilder::concat
        )
      }

      val androidResSource: Set<AndroidResSource> = builders.values.asSequence()
        .map { it.build() }
        .toSortedSet()
      val androidResRuntimeSource: Set<AndroidResSource> = runtimeBuilders.values.asSequence()
        .map { it.build() }
        .toSortedSet()

      output.bufferWriteJsonSet(androidResSource)
      outputRuntime.bufferWriteJsonSet(androidResRuntimeSource)
    }
  }
}
