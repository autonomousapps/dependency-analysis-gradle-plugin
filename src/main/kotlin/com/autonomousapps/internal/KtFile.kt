package com.autonomousapps.internal

import kotlinx.metadata.jvm.KotlinModuleMetadata
import java.util.zip.ZipFile

/**
 * A "KT File" is one that has top-level declarations, and so the class file is something like
 * `com.example.ThingKt`, but imports in Kotlin code look like `com.example.CONSTANT` (rather than
 * `com.example.ThingKt.CONSTANT`).
 */
data class KtFile(
  val fqcn: String,
  val name: String
) {
  companion object {
    fun fromZip(zipFile: ZipFile): List<KtFile> =
      zipFile.entries().toList().find {
        it.name.endsWith(".kotlin_module")
      }?.let {
        val bytes = zipFile.getInputStream(it).readBytes()
        val metadata = KotlinModuleMetadata.read(bytes)
        val module = metadata?.toKmModule()
        module?.packageParts?.flatMap { (packageName, parts) ->
          parts.fileFacades.map { facade ->
            // com/example/library/ConstantsKt --> [com.example.library.ConstantsKt, ConstantsKt]
            val fqcn = facade.replace("/", ".")
            KtFile(fqcn, fqcn.removePrefix("$packageName."))
          }
        }
      } ?: emptyList()
  }
}
