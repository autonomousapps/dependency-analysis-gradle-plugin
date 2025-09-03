// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage") // AGP stuff

package com.autonomousapps.internal.analyzer

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Sources
import com.android.build.api.variant.Variant
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.tasks.AndroidClassesTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal interface AndroidSources {
  val sources: Sources

  val sourceKind: AndroidSourceKind

  /** E.g., `debugCompileClasspath` or `debugUnitTestCompileClasspath` */
  val compileClasspathConfigurationName: String

  /** E.g., `debugRuntimeClasspath` or `debugUnitTestRuntimeClasspath` */
  val runtimeClasspathConfigurationName: String

  fun getAndroidAssets(): Provider<Iterable<File>>

  /** Manifests stored in source, or perhaps generated. cf [getMergedManifest]. */
  fun getManifestFiles(): Provider<out List<RegularFile>>

  /** Only ever a single file, but returning a list makes it easy to return an _empty_ list. cf [getManifestFiles]. */
  fun getMergedManifest(): Provider<Iterable<File>>

  fun getAndroidRes(): Provider<List<Collection<Directory>>>?
  fun wireWithClassFiles(task: TaskProvider<out AndroidClassesTask>)
}

/** For `com.android.application` and `com.android.library` projects' _main_ source. */
internal open class DefaultAndroidSources(
  private val project: Project,
  /**
   * "Primary" as opposed to UnitTest or AndroidTest sub-variants.
   *
   * @see [Variant.unitTest]
   * @see [com.android.build.api.variant.HasAndroidTest.androidTest]
   */
  private val primaryAgpVariant: Variant,

  /**
   * The artifacts accessor for the specific sub-variant that this `AndroidSources` instance defines. May be the
   * production artifacts (main/debug/release/etc), or the test or androidTest sources.
   */
  private val agpArtifacts: Artifacts,
  override val sources: Sources,
  override val sourceKind: AndroidSourceKind,
  override val compileClasspathConfigurationName: String,
  override val runtimeClasspathConfigurationName: String,
) : AndroidSources {

  final override fun getAndroidAssets(): Provider<Iterable<File>> {
    return sources.assets?.all
      ?.map { layers -> layers.flatten() }
      ?.map { directories -> directories.map { directory -> directory.asFileTree } }
      ?.map { trees -> trees.flatten() }
      ?: project.provider { emptyList() }
  }

  /**
   * nb: android res is a superset of layouts. This means layouts will get parsed twice. This is currently simpler
   * than rewriting a bunch of code (the two parsers look for different things).
   */
  override fun getAndroidRes(): Provider<List<Collection<Directory>>>? {
    return sources.res?.all
  }

  override fun getManifestFiles(): Provider<out List<RegularFile>> {
    return sources.manifests.all
  }

  // For this one, we want to use the main variant's artifacts
  override fun getMergedManifest(): Provider<Iterable<File>> {
    return primaryAgpVariant.artifacts.get(SingleArtifact.MERGED_MANIFEST).map { listOf(it.asFile) }
  }

  final override fun wireWithClassFiles(task: TaskProvider<out AndroidClassesTask>) {
    // For this one, we want to use the main/test/androidTest variant's artifacts, depending on the source set under
    // analysis.
    agpArtifacts.forScope(ScopedArtifacts.Scope.PROJECT)
      .use(task)
      .toGet(
        type = ScopedArtifact.CLASSES,
        inputJars = AndroidClassesTask::jars,
        inputDirectories = AndroidClassesTask::dirs,
      )
  }
}

/**
 * For `com.android.application` and `com.android.library` projects' _test_ source.
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1111">DAGP Issue 1111</a>
 * @see <a href="https://issuetracker.google.com/issues/325307775>Google issue 325307775</a>
 */
internal class TestAndroidSources(
  private val project: Project,
  primaryAgpVariant: Variant,
  agpArtifacts: Artifacts,
  sources: Sources,
  sourceKind: AndroidSourceKind,
  compileClasspathConfigurationName: String,
  runtimeClasspathConfigurationName: String,
) : DefaultAndroidSources(
  project,
  primaryAgpVariant,
  agpArtifacts,
  sources,
  sourceKind,
  compileClasspathConfigurationName,
  runtimeClasspathConfigurationName,
) {
  override fun getAndroidRes(): Provider<List<Collection<Directory>>>? = null
  override fun getManifestFiles(): Provider<out List<RegularFile>> = project.provider { emptyList() }
  override fun getMergedManifest(): Provider<Iterable<File>> = project.provider { emptyList() }
}

/**
 * For `com.android.test` projects.
 *
 * I don't fully understand why [com.autonomousapps.tasks.XmlSourceExploderTask] fails with the following error, but
 * overriding [getManifestFiles] to return an empty list resolves it.
 *
 * ```
 * * What went wrong:
 * Could not determine the dependencies of task ':benchmark:explodeXmlSourceDebug'.
 * > Cannot query the value of this provider because it has no value available.
 * ```
 *
 * See `com.autonomousapps.androidAndroidTestSmokeSpec`.
 */
internal class ComAndroidTestAndroidSources(
  private val project: Project,
  primaryAgpVariant: Variant,
  agpArtifacts: Artifacts,
  sources: Sources,
  sourceKind: AndroidSourceKind,
  compileClasspathConfigurationName: String,
  runtimeClasspathConfigurationName: String,
) : DefaultAndroidSources(
  project,
  primaryAgpVariant,
  agpArtifacts,
  sources,
  sourceKind,
  compileClasspathConfigurationName,
  runtimeClasspathConfigurationName,
) {
  override fun getManifestFiles(): Provider<out List<RegularFile>> = project.provider { emptyList() }
  override fun getMergedManifest(): Provider<Iterable<File>> = project.provider { emptyList() }
}
