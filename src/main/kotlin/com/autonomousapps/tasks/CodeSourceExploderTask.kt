// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.parse.SourceListener
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.CodeSource.Kind
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingSourceCode
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

@CacheableTask
public abstract class CodeSourceExploderTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val layout: ProjectLayout
) : DefaultTask() {

  init {
    description = "Parses Java and Kotlin source to detect source-only usages"
  }

  /** The Groovy source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val groovySourceFiles: ConfigurableFileCollection

  /** The Java source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val javaSourceFiles: ConfigurableFileCollection

  /** The Kotlin source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val kotlinSourceFiles: ConfigurableFileCollection

  /** The Scala source of the current project. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val scalaSourceFiles: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(CodeSourceExploderWorkAction::class.java) {
      projectDir.set(layout.projectDirectory)
      groovySourceFiles.from(this@CodeSourceExploderTask.groovySourceFiles)
      javaSourceFiles.from(this@CodeSourceExploderTask.javaSourceFiles)
      kotlinSourceFiles.from(this@CodeSourceExploderTask.kotlinSourceFiles)
      scalaSourceFiles.from(this@CodeSourceExploderTask.scalaSourceFiles)
      output.set(this@CodeSourceExploderTask.output)
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

  /**
   * TODO(tsr): the file-existence checks are only necessary for Android projects. It is currently unclear why.
   *  [AndroidSources][com.autonomousapps.internal.analyzer.AndroidSources] may be buggy, or possibly AGP is buggy. This
   *  may also be a source of more ~cache-adjacent bugs.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/pull/1534">PR 1534</a>
   */
  fun explode(): Set<ExplodingSourceCode> {
    val destination = sortedSetOf<ExplodingSourceCode>()
    javaSourceFiles
      .filter(File::exists)
      .mapTo(destination) { f ->
        val rel = relativize(f)
        ExplodingSourceCode(
          relativePath = rel,
          className = canonicalClassName(rel),
          kind = Kind.JAVA,
          imports = SourceListener.parseSourceFileForImports(f)
        )
      }
    kotlinSourceFiles
      .filter(File::exists)
      .mapTo(destination) { f ->
        val rel = relativize(f)
        ExplodingSourceCode(
          relativePath = rel,
          className = canonicalClassName(rel),
          kind = Kind.KOTLIN,
          imports = SourceListener.parseSourceFileForImports(f)
        )
      }
    groovySourceFiles
      .filter(File::exists)
      .mapTo(destination) { f ->
        val rel = relativize(f)
        ExplodingSourceCode(
          relativePath = rel,
          className = canonicalClassName(rel),
          kind = Kind.GROOVY,
          imports = SourceListener.parseSourceFileForImports(f)
        )
      }
    scalaSourceFiles
      .filter(File::exists)
      .mapTo(destination) { f ->
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
