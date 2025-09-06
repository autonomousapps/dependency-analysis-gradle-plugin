// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.analyzer.Language
import com.autonomousapps.internal.parse.AndroidLayoutParser
import com.autonomousapps.internal.parse.AndroidManifestParser
import com.autonomousapps.internal.parse.AndroidResBuilder
import com.autonomousapps.internal.parse.AndroidResParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.AndroidResSource
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
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
public abstract class XmlSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout,
) : DefaultTask() {

  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val androidLocalRes: ListProperty<Collection<Directory>>

  /** AndroidManifest.xml files. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val manifests: ListProperty<RegularFile>

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

  private fun getAndroidRes(): Iterable<File> {
    return androidLocalRes.orNull
      ?.flatMap { dirs ->
        dirs.flatMap { dir ->
          dir
            .asFileTree
            // Sometimes there's weird nonsense in the layout directories
            .matching(Language.filterOf(Language.XML))
            .files
        }
      }
      .orEmpty()
  }

  /** Android layout XML files. */
  private fun getLayoutFiles(): Iterable<File> {
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1112
    // https://issuetracker.google.com/issues/325307775
    return try {
      androidLocalRes.orNull
        ?.flatMap { dirs ->
          dirs.flatMap { dir ->
            dir
              .asFileTree
              .matching { it.include("**/layout/**/*.xml") }
              .files
          }
        }
        .orEmpty()
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun getManifestFiles(): Iterable<File> {
    return manifests.get().map { manifest -> manifest.asFile }
  }

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(XmlSourceExploderWorkAction::class.java) {
      it.projectDir.set(layout.projectDirectory)
      it.androidRes.setFrom(getAndroidRes())
      it.layouts.setFrom(getLayoutFiles())
      it.manifests.setFrom(getManifestFiles())
      it.mergedManifests.setFrom(mergedManifestFiles)
      it.namespace.set(namespace)
      it.output.set(output)
      it.outputRuntime.set(outputRuntime)
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
