// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.squareup.moshi.JsonClass
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi

/**
 * A "KT File" is one that has top-level declarations, and so the class file is something like
 * `com.example.ThingKt`, but imports in Kotlin code look like `com.example.CONSTANT` (rather than
 * `com.example.ThingKt.CONSTANT`).
 */
@JsonClass(generateAdapter = false)
internal data class KtFile(
  val fqcn: String,
  val name: String
) : Comparable<KtFile> {

  override fun compareTo(other: KtFile): Int {
    return compareBy(KtFile::fqcn)
      .thenComparing(compareBy(KtFile::name))
      .compare(this, other)
  }

  internal companion object {
    private const val KOTLIN_MODULE = ".kotlin_module"

    fun fromDirectory(dir: File): Set<KtFile> {
      check(dir.isDirectory) { "Expected directory. Was '${dir.absolutePath}'" }

      return dir
        .walkBottomUp()
        .firstOrNull { it.name.endsWith(KOTLIN_MODULE) }
        ?.let { fromFile(it) }
        .orEmpty()
    }

    fun fromZip(zipFile: ZipFile): Set<KtFile> {
      return zipFile.entries()
        .toList()
        .find { it.name.endsWith(KOTLIN_MODULE) }
        ?.let { fromInputStream(zipFile.getInputStream(it)) }
        .orEmpty()
    }

    private fun fromFile(file: File): Set<KtFile> = fromInputStream(file.inputStream())

    @OptIn(UnstableMetadataApi::class)
    private fun fromInputStream(input: InputStream): Set<KtFile> {
      val bytes = input.use { it.readBytes() }
      val metadata = KotlinModuleMetadata.read(bytes)
      val module = metadata.kmModule

      return module.packageParts.flatMap { (packageName, parts) ->
        parts.fileFacades.map { facade ->
          // com/example/library/ConstantsKt --> [com.example.library.ConstantsKt, ConstantsKt]
          val fqcn = facade.replace('/', '.')
          KtFile(fqcn, fqcn.removePrefix("$packageName."))
        }
      }.toSortedSet()
    }
  }
}
