@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.VariantFile
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import java.io.File

abstract class CreateVariantFiles : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates the VariantFile objects needed for later analysis"
  }

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()
    outputFile.writeText(variantFiles().toJson())
  }

  abstract fun variantFiles(): Set<VariantFile>

  protected fun Set<File>.toVariantFiles(name: String): Set<VariantFile> {
    return asSequence().map { file ->
      project.relativePath(file)
    }.map { it.removePrefix("src/$name/") }
      // remove java/, kotlin/ and /res from start
      .map { it.substring(it.indexOf("/") + 1) }
      // remove file extension from end
      .mapNotNull {
        val index = it.lastIndexOf(".")
        if (index != -1) {
          it.substring(0, index)
        } else {
          // This could happen if the directory were empty, (eg `src/main/java/` with nothing in it)
          null
        }
      }
      .map { VariantFile(name, it) }
      .toSet()
  }
}

data class CollectionHolder(
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  val collection: ConfigurableFileCollection
)

@CacheableTask
abstract class AndroidCreateVariantFiles : CreateVariantFiles() {

  @get:Nested
  abstract val namedJavaDirs: MapProperty<String, CollectionHolder>

  @get:Nested
  abstract val namedKotlinDirs: MapProperty<String, CollectionHolder>

  @get:Nested
  abstract val namedXmlDirs: MapProperty<String, CollectionHolder>

  override fun variantFiles(): Set<VariantFile> {
    val javaVariantFiles: Set<VariantFile> = namedJavaDirs.get()
      .flatMapTo(mutableSetOf()) { (name, holder) ->
        holder.collection.asFileTree.files.toVariantFiles(name)
      }
    val kotlinVariantFiles: Set<VariantFile> = namedKotlinDirs.get()
      .flatMapTo(mutableSetOf()) { (name, holder) ->
        holder.collection.asFileTree.files.toVariantFiles(name)
      }
    val xmlVariantFiles: Set<VariantFile> = namedXmlDirs.get()
      .flatMapTo(mutableSetOf()) { (name, holder) ->
        holder.collection.asFileTree.files.toVariantFiles(name)
      }

    return javaVariantFiles + kotlinVariantFiles + xmlVariantFiles
  }
}

@CacheableTask
abstract class JvmCreateVariantFiles : CreateVariantFiles() {

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val mainFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  @get:InputFiles
  abstract val testFiles: ConfigurableFileCollection

  override fun variantFiles(): Set<VariantFile> {
    val mainFiles = mainFiles.asFileTree.files.toVariantFiles("main")
    val testFiles = testFiles.asFileTree.files.toVariantFiles("test")

    return mainFiles + testFiles
  }
}
