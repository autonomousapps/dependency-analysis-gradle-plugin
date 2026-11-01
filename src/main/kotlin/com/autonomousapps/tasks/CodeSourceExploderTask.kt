// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.analyzer.Language
import com.autonomousapps.internal.parse.SourceListener
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.fromNullableJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.CodeSource.Kind
import com.autonomousapps.model.internal.intermediates.consumer.ExplodingSourceCode
import com.autonomousapps.model.internal.kmp.SourceSetFiles
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
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

  /** Only available in KMP projects. */
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val kmpSources: RegularFileProperty

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
      it.kmpSources.set(kmpSources)
      it.groovySourceFiles.from(getGroovySourceFiles())
      it.javaSourceFiles.from(getJavaSourceFiles())
      it.kotlinSourceFiles.from(getKotlinSourceFiles())
      it.scalaSourceFiles.from(getScalaSourceFiles())
      it.output.set(output)
    }
  }

  public interface CodeSourceExploderParameters : WorkParameters {
    public val projectDir: DirectoryProperty
    public val kmpSources: RegularFileProperty
    public val groovySourceFiles: ConfigurableFileCollection
    public val javaSourceFiles: ConfigurableFileCollection
    public val kotlinSourceFiles: ConfigurableFileCollection
    public val scalaSourceFiles: ConfigurableFileCollection
    public val output: RegularFileProperty
  }

  public abstract class CodeSourceExploderWorkAction : WorkAction<CodeSourceExploderParameters> {

    override fun execute() {
      val reportFile = parameters.output.getAndDelete()

      // may be empty
      val kmpSources = parameters.kmpSources.fromNullableJsonSet<SourceSetFiles>()

      val explodedSource = SourceExploder(
        projectDir = parameters.projectDir.get().asFile,
        kmpSources = kmpSources,
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
  private val kmpSources: Set<SourceSetFiles>,
  private val groovySourceFiles: ConfigurableFileCollection,
  private val javaSourceFiles: ConfigurableFileCollection,
  private val kotlinSourceFiles: ConfigurableFileCollection,
  private val scalaSourceFiles: ConfigurableFileCollection,
) {

  fun explode(): Set<ExplodingSourceCode> {
    val destination = sortedSetOf<ExplodingSourceCode>()

    // TODO(tsr): might need this in multiple places
    // For KMP projects, we associate the analyzed class file with the source file on disk, to bridge the compilation-
    // source set gap. This value should always be non-null for KMP projects, and null otherwise.
//    val kmpSourceSetName = kmpSources
//      .find { sources ->
//        sources.sourceFiles.any { f ->
//          // a standard source/class file
//          f.removeSuffix(".kt") == classAnalyzer.className
//            // a class created from top-level functions in the root package
//            || f == classAnalyzer.source
//            // a class created from top-level functions in a non-root package
//            || f == classAnalyzer.className.substringBeforeLast('/') + '/' + classAnalyzer.source
//        }
//      }
//      ?.sourceSetName


    javaSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      val sourceListener = SourceListener.parseSourceFile(f)
      val pkg = packageFrom(sourceListener)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(pkg, f),
        kind = Kind.JAVA,
        imports = importsFrom(sourceListener),
        kmpSourceSetName = null,
      )
    }
    kotlinSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      val sourceListener = SourceListener.parseSourceFile(f)
      val pkg = packageFrom(sourceListener)

      // TODO(tsr): might need this in multiple places
      val kmpSourceSetName = kmpSources
        .find { sources ->
          // TODO: handle other kinds of files:
          //  1. a class created from top-level functions in the root package: `f == classAnalyzer.source`
          //  2. a class created from top-level functions in a non-root package: `f == classAnalyzer.className.substringBeforeLast('/') + '/' + classAnalyzer.source`
          sources.sourceFiles.any { f ->
            // src/commonMain/kotlin + / + com/foo/Bar.kt
            "${sources.sourceBase}/$f" == rel
          }
        }
        ?.sourceSetName

      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(pkg, f),
        kind = Kind.KOTLIN,
        imports = importsFrom(sourceListener),
        kmpSourceSetName = kmpSourceSetName,
      )
    }
    groovySourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      val sourceListener = SourceListener.parseSourceFile(f)
      val pkg = packageFrom(sourceListener)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(pkg, f),
        kind = Kind.GROOVY,
        imports = importsFrom(sourceListener),
        kmpSourceSetName = null,
      )
    }
    scalaSourceFiles.mapTo(destination) { f ->
      val rel = relativize(f)
      val sourceListener = SourceListener.parseSourceFile(f)
      val pkg = packageFrom(sourceListener)
      ExplodingSourceCode(
        relativePath = rel,
        className = canonicalClassName(pkg, f),
        kind = Kind.SCALA,
        imports = importsFrom(sourceListener),
        kmpSourceSetName = null,
      )
    }

    return destination
  }

  private fun relativize(file: File) = file.toRelativeString(projectDir)

  private fun canonicalClassName(pkg: String?, file: File): String {
    // Foo.java -> Foo
    val name = file.name.substringBeforeLast('.')
    return if (pkg != null) {
      "$pkg.$name"
    } else {
      name
    }
  }

  /*
   * nb: the "cannot access..." IDE error is incorrect. It's failing at something to do with shaded packages yet again.
   * I put these functions down here to consolidate the errors.
   */

  private fun packageFrom(sourceListener: SourceListener): String? = sourceListener.packageDeclaration()
  private fun importsFrom(sourceListener: SourceListener): Set<String> = sourceListener.imports()
}
