// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.analyzer.Language
import com.autonomousapps.internal.parse.SourceListener
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.CodeSource.Kind
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingSourceCode
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

@CacheableTask
public abstract class JvmCodeSourceExploderTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  layout: ProjectLayout
) : CodeSourceExploderTask(workerExecutor, layout) {

  /** The Groovy source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val groovySource: ConfigurableFileCollection

  /** The Java source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val javaSource: ConfigurableFileCollection

  /** The Kotlin source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val kotlinSource: ConfigurableFileCollection

  /** The Scala source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val scalaSource: ConfigurableFileCollection

  final override fun getGroovySourceFiles(): Iterable<File> {
    return groovySource.asFileTree.matching(Language.filterOf(Language.GROOVY))
  }

  final override fun getJavaSourceFiles(): Iterable<File> {
    return javaSource.asFileTree.matching(Language.filterOf(Language.JAVA))
  }

  final override fun getKotlinSourceFiles(): Iterable<File> {
    return kotlinSource.asFileTree.matching(Language.filterOf(Language.KOTLIN))
  }

  final override fun getScalaSourceFiles(): Iterable<File> {
    return scalaSource.asFileTree.matching(Language.filterOf(Language.SCALA))
  }
}

@CacheableTask
public abstract class AndroidCodeSourceExploderTask @Inject constructor(
  workerExecutor: WorkerExecutor,
  layout: ProjectLayout
) : CodeSourceExploderTask(workerExecutor, layout) {

  /** The Java source of the current project. */
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val javaSource: ListProperty<Directory>

  /** The Kotlin source of the current project. */
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val kotlinSource: ListProperty<Directory>

  final override fun getJavaSourceFiles(): Iterable<File> {
    return javaSource.orNull
      ?.flatMap { dir -> dir.asFileTree.matching(Language.filterOf(Language.JAVA)) }
      .orEmpty()
  }

  final override fun getKotlinSourceFiles(): Iterable<File> {
    return kotlinSource.orNull
      ?.flatMap { dir -> dir.asFileTree.matching(Language.filterOf(Language.KOTLIN)) }
      .orEmpty()
  }

  /*
   * Android projects only support Java and Kotlin.
   */

  final override fun getGroovySourceFiles(): Iterable<File> = emptyList()
  final override fun getScalaSourceFiles(): Iterable<File> = emptyList()
}

@CacheableTask
public abstract class CodeSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout
) : DefaultTask() {

  init {
    description = "Parses Java and Kotlin source to detect source-only usages"
  }

  /** The Groovy source of the current project. */
  @Internal
  protected abstract fun getGroovySourceFiles(): Iterable<File>

  /** The Java source of the current project. */
  @Internal
  protected abstract fun getJavaSourceFiles(): Iterable<File>

  /** The Kotlin source of the current project. */
  @Internal
  protected abstract fun getKotlinSourceFiles(): Iterable<File>

  /** The Scala source of the current project. */
  @Internal
  protected abstract fun getScalaSourceFiles(): Iterable<File>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(CodeSourceExploderWorkAction::class.java) {
      it.projectDir.set(layout.projectDirectory)
      it.groovySourceFiles.from(getGroovySourceFiles())
      it.javaSourceFiles.from(getJavaSourceFiles())
      it.kotlinSourceFiles.from(getKotlinSourceFiles())
      it.scalaSourceFiles.from(getScalaSourceFiles())
      it.output.set(output)
    }
  }

  public interface CodeSourceExploderParameters : WorkParameters {
    public val projectDir: DirectoryProperty
    public val groovySourceFiles: ConfigurableFileCollection
    public val javaSourceFiles: ConfigurableFileCollection
    public val kotlinSourceFiles: ConfigurableFileCollection
    public val scalaSourceFiles: ConfigurableFileCollection
    public val output: RegularFileProperty
  }

  public abstract class CodeSourceExploderWorkAction : WorkAction<CodeSourceExploderParameters> {

    override fun execute() {
      val reportFile = parameters.output.getAndDelete()

      val explodedSource = SourceExploder(
        projectDir = parameters.projectDir.get().asFile,
        groovySourceFiles = parameters.groovySourceFiles,
        javaSourceFiles = parameters.javaSourceFiles,
        kotlinSourceFiles = parameters.kotlinSourceFiles,
        scalaSourceFiles = parameters.scalaSourceFiles,
      ).explode()

      reportFile.bufferWriteJsonSet(explodedSource)
    }
  }
}

private class SourceExploder(
  private val projectDir: File,
  private val groovySourceFiles: ConfigurableFileCollection,
  private val javaSourceFiles: ConfigurableFileCollection,
  private val kotlinSourceFiles: ConfigurableFileCollection,
  private val scalaSourceFiles: ConfigurableFileCollection,
) {

  fun explode(): Set<ExplodingSourceCode> {
    val destination = sortedSetOf<ExplodingSourceCode>()
    javaSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(rel),
        kind = Kind.JAVA,
        imports = SourceListener.parseSourceFileForImports(f)
      )
    }
    kotlinSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(rel),
        kind = Kind.KOTLIN,
        imports = SourceListener.parseSourceFileForImports(f)
      )
    }
    groovySourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(rel),
        kind = Kind.GROOVY,
        imports = SourceListener.parseSourceFileForImports(f)
      )
    }
    scalaSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(rel),
        kind = Kind.SCALA,
        imports = SourceListener.parseSourceFileForImports(f)
      )
    }

    return destination
  }

  private fun relativize(file: File) = file.toRelativeString(projectDir)

  private fun canonicalClassName(relativePath: String): String {
    return Paths.get(relativePath)
      // Hack to drop e.g. `src/main/java`. Would be better if a FileTree exposed that info.
      .drop(3)
      // com/example/Foo.java -> com.example.Foo.java
      .joinToString(separator = ".")
      // Drop file extension (.java, .kt, etc.) as well.
      .substringBeforeLast('.')
  }
}
