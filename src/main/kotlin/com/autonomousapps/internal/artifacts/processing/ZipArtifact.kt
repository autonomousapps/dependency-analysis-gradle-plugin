// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.artifacts.processing

import com.autonomousapps.internal.utils.asSequenceOfClassFiles
import com.autonomousapps.model.internal.KtFile
import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

internal class ZipArtifact private constructor(private val zipFile: ZipFile) : Artifact, Closeable by zipFile {

  constructor(file: File) : this(ZipFile(file))

  override fun hasKotlinClasses(): Boolean = KtFile.hasKotlinClasses(zipFile)

  override fun ktFiles(): Set<KtFile> = KtFile.fromZip(zipFile)

  override fun asSequenceOfClassFiles(): Sequence<ClassFile> =
    zipFile.asSequenceOfClassFiles().map { entry ->
      ClassFile(
        inputStreamSupplier = { zipFile.getInputStream(entry) },
        packagePath = entry.name
      )
    }
}
